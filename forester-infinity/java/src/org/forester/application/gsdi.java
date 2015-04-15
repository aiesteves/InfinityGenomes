// $Id:
// FORESTER -- software libraries and applications
// for evolutionary biology research and applications.
//
// Copyright (C) 2008-2009 Christian M. Zmasek
// Copyright (C) 2008-2009 Burnham Institute for Medical Research
// All rights reserved
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
//
// Contact: phylosoft @ gmail . com
// WWW: www.phylosoft.org/forester

package org.forester.application;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.forester.io.parsers.PhylogenyParser;
import org.forester.io.parsers.nhx.NHXParser;
import org.forester.io.parsers.phyloxml.PhyloXmlDataFormatException;
import org.forester.io.parsers.phyloxml.PhyloXmlParser;
import org.forester.io.parsers.util.ParserUtils;
import org.forester.io.writers.PhylogenyWriter;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyMethods;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.factories.ParserBasedPhylogenyFactory;
import org.forester.phylogeny.factories.PhylogenyFactory;
import org.forester.sdi.GSDI;
import org.forester.sdi.SDI;
import org.forester.sdi.SDI.TaxonomyComparisonBase;
import org.forester.sdi.SDIException;
import org.forester.sdi.SDIse;
import org.forester.util.CommandLineArguments;
import org.forester.util.EasyWriter;
import org.forester.util.ForesterConstants;
import org.forester.util.ForesterUtil;

public final class gsdi {

    private enum BASE_ALGORITHM {
        GSDI, SDI
    }
    final static public boolean REPLACE_UNDERSCORES_IN_NH_SPECIES_TREE = true;
    final static private String ALLOW_STRIPPING_OF_GENE_TREE_OPTION    = "g";
    final static private String SDISE_OPTION                           = "b";
    final static private String MOST_PARSIMONIOUS_OPTION               = "m";
    final static private String GUESS_FORMAT_OF_SPECIES_TREE           = "q";
    final static private String HELP_OPTION_1                          = "help";
    final static private String HELP_OPTION_2                          = "h";
    final static private String SUFFIX_FOR_SPECIES_TREE_USED           = "_species_tree_used.xml";
    final static private String LOGFILE_SUFFIX                         = "_gsdi_log.txt";
    final static private String REMAPPED_SUFFIX                        = "_gsdi_remapped.txt";
    final static private String PRG_NAME                               = "gsdi";
    final static private String PRG_VERSION                            = "1.000";
    final static private String PRG_DATE                               = "120629";
    final static private String PRG_DESC                               = "general speciation duplication inference";
    final static private String E_MAIL                                 = "phylosoft@gmail.com";
    final static private String WWW                                    = "www.phylosoft.org/forester";

    public static void main( final String args[] ) {
        try {
            ForesterUtil.printProgramInformation( PRG_NAME,
                                                  PRG_DESC,
                                                  PRG_VERSION,
                                                  PRG_DATE,
                                                  E_MAIL,
                                                  WWW,
                                                  ForesterUtil.getForesterLibraryInformation() );
            CommandLineArguments cla = null;
            try {
                cla = new CommandLineArguments( args );
            }
            catch ( final Exception e ) {
                ForesterUtil.fatalError( PRG_NAME, e.getMessage() );
            }
            if ( cla.isOptionSet( gsdi.HELP_OPTION_1 ) || cla.isOptionSet( gsdi.HELP_OPTION_2 ) ) {
                System.out.println();
                gsdi.print_help();
                System.exit( 0 );
            }
            else if ( ( args.length < 2 ) || ( cla.getNumberOfNames() != 3 ) ) {
                System.out.println();
                System.out.println( "Wrong number of arguments." );
                System.out.println();
                gsdi.print_help();
                System.exit( -1 );
            }
            final List<String> allowed_options = new ArrayList<String>();
            allowed_options.add( gsdi.SDISE_OPTION );
            allowed_options.add( gsdi.GUESS_FORMAT_OF_SPECIES_TREE );
            allowed_options.add( gsdi.MOST_PARSIMONIOUS_OPTION );
            allowed_options.add( gsdi.ALLOW_STRIPPING_OF_GENE_TREE_OPTION );
            final String dissallowed_options = cla.validateAllowedOptionsAsString( allowed_options );
            if ( dissallowed_options.length() > 0 ) {
                ForesterUtil.fatalError( gsdi.PRG_NAME, "unknown option(s): " + dissallowed_options );
            }
            execute( cla );
        }
        catch ( final IOException e ) {
            ForesterUtil.fatalError( gsdi.PRG_NAME, e.getMessage() );
        }
    }

    private static void execute( final CommandLineArguments cla ) throws IOException {
        BASE_ALGORITHM base_algorithm = BASE_ALGORITHM.GSDI;
        boolean most_parsimonous_duplication_model = false;
        boolean allow_stripping_of_gene_tree = false;
        if ( cla.isOptionSet( gsdi.SDISE_OPTION ) ) {
            base_algorithm = BASE_ALGORITHM.SDI;
        }
        if ( cla.isOptionSet( gsdi.MOST_PARSIMONIOUS_OPTION ) ) {
            if ( base_algorithm != BASE_ALGORITHM.GSDI ) {
                ForesterUtil.fatalError( gsdi.PRG_NAME, "Can only use most parsimonious duplication mode with GSDI" );
            }
            most_parsimonous_duplication_model = true;
        }
        if ( cla.isOptionSet( gsdi.ALLOW_STRIPPING_OF_GENE_TREE_OPTION ) ) {
            if ( base_algorithm != BASE_ALGORITHM.GSDI ) {
                ForesterUtil.fatalError( gsdi.PRG_NAME, "Can only allow stripping of gene tree with GSDI" );
            }
            allow_stripping_of_gene_tree = true;
        }
        Phylogeny species_tree = null;
        Phylogeny gene_tree = null;
        File gene_tree_file = null;
        File species_tree_file = null;
        File out_file = null;
        File log_file = null;
        EasyWriter log_writer = null;
        try {
            gene_tree_file = cla.getFile( 0 );
            species_tree_file = cla.getFile( 1 );
            out_file = cla.getFile( 2 );
            log_file = new File( ForesterUtil.removeSuffix( out_file.toString() ) + LOGFILE_SUFFIX );
        }
        catch ( final IllegalArgumentException e ) {
            ForesterUtil.fatalError( gsdi.PRG_NAME, "error in command line: " + e.getMessage() );
        }
        if ( ForesterUtil.isReadableFile( gene_tree_file ) != null ) {
            ForesterUtil.fatalError( gsdi.PRG_NAME, ForesterUtil.isReadableFile( gene_tree_file ) );
        }
        if ( ForesterUtil.isReadableFile( species_tree_file ) != null ) {
            ForesterUtil.fatalError( gsdi.PRG_NAME, ForesterUtil.isReadableFile( species_tree_file ) );
        }
        if ( ForesterUtil.isWritableFile( out_file ) != null ) {
            ForesterUtil.fatalError( gsdi.PRG_NAME, ForesterUtil.isWritableFile( out_file ) );
        }
        if ( ForesterUtil.isWritableFile( log_file ) != null ) {
            ForesterUtil.fatalError( gsdi.PRG_NAME, ForesterUtil.isWritableFile( log_file ) );
        }
        try {
            log_writer = ForesterUtil.createEasyWriter( log_file );
        }
        catch ( final IOException e ) {
            ForesterUtil.fatalError( gsdi.PRG_NAME, "Failed to create [" + log_file + "]: " + e.getMessage() );
        }
        try {
            final PhylogenyFactory factory = ParserBasedPhylogenyFactory.getInstance();
            gene_tree = factory.create( gene_tree_file, new PhyloXmlParser() )[ 0 ];
        }
        catch ( final IOException e ) {
            fatalError( "ERROR",
                        "Failed to read gene tree from [" + gene_tree_file + "]: " + e.getMessage(),
                        log_writer );
        }
        try {
            final PhylogenyFactory factory = ParserBasedPhylogenyFactory.getInstance();
            final PhylogenyParser p = ParserUtils.createParserDependingOnFileType( species_tree_file, true );
            if ( p instanceof PhyloXmlParser ) {
                species_tree = factory.create( species_tree_file, p )[ 0 ];
            }
            else {
                if ( REPLACE_UNDERSCORES_IN_NH_SPECIES_TREE && ( p instanceof NHXParser ) ) {
                    ( ( NHXParser ) p ).setReplaceUnderscores( true );
                }
                species_tree = factory.create( species_tree_file, p )[ 0 ];
                final TaxonomyComparisonBase comp_base = GSDI.determineTaxonomyComparisonBase( gene_tree );
                switch ( comp_base ) {
                    case SCIENTIFIC_NAME:
                        try {
                            PhylogenyMethods
                                    .transferNodeNameToField( species_tree,
                                                              PhylogenyMethods.PhylogenyNodeField.TAXONOMY_SCIENTIFIC_NAME,
                                                              true );
                        }
                        catch ( final PhyloXmlDataFormatException e ) {
                            fatalError( "USER ERROR", "Failed to transfer general node name to scientific name, in ["
                                    + species_tree_file + "]: " + e.getMessage(), log_writer );
                        }
                        break;
                    case CODE:
                        try {
                            PhylogenyMethods
                                    .transferNodeNameToField( species_tree,
                                                              PhylogenyMethods.PhylogenyNodeField.TAXONOMY_CODE,
                                                              true );
                        }
                        catch ( final PhyloXmlDataFormatException e ) {
                            fatalError( "USER ERROR", "Failed to transfer general node name to taxonomy code, in ["
                                    + species_tree_file + "]: " + e.getMessage(), log_writer );
                        }
                        break;
                    case ID:
                        try {
                            PhylogenyMethods.transferNodeNameToField( species_tree,
                                                                      PhylogenyMethods.PhylogenyNodeField.TAXONOMY_ID,
                                                                      true );
                        }
                        catch ( final PhyloXmlDataFormatException e ) {
                            fatalError( "USER ERROR", "Failed to transfer general node name to taxonomy id, in ["
                                    + species_tree_file + "]: " + e.getMessage(), log_writer );
                        }
                        break;
                    default:
                        fatalError( "UNEXPECTED ERROR", "unable to determine comparison base", log_writer );
                }
            }
        }
        catch ( final IOException e ) {
            fatalError( "ERROR",
                        "Failed to read species tree from [" + species_tree_file + "]: " + e.getMessage(),
                        log_writer );
        }
        gene_tree.setRooted( true );
        species_tree.setRooted( true );
        if ( !gene_tree.isCompletelyBinary() ) {
            fatalError( "user error", "gene tree is not completely binary", log_writer );
        }
        if ( base_algorithm != BASE_ALGORITHM.GSDI ) {
            if ( !species_tree.isCompletelyBinary() ) {
                fatalError( "user error", "species tree is not completely binary, use GSDI instead", log_writer );
            }
        }
        log_writer.println( PRG_NAME + " - " + PRG_DESC );
        log_writer.println( "  version         : " + PRG_VERSION );
        log_writer.println( "  date            : " + PRG_DATE );
        log_writer.println( "  forester version: " + ForesterConstants.FORESTER_VERSION );
        log_writer.println();
        log_writer.println( "Start time                               : "
                + new SimpleDateFormat( "yyyyMMdd HH:mm:ss" ).format( new Date() ) );
        System.out.println( "Start time                               : "
                + new SimpleDateFormat( "yyyyMMdd HH:mm:ss" ).format( new Date() ) );
        log_writer.println( "Gene tree file                           : " + gene_tree_file.getCanonicalPath() );
        System.out.println( "Gene tree file                           : " + gene_tree_file.getCanonicalPath() );
        log_writer.println( "Gene tree name                           : "
                + ( ForesterUtil.isEmpty( gene_tree.getName() ) ? "" : gene_tree.getName() ) );
        System.out.println( "Gene tree name                           : "
                + ( ForesterUtil.isEmpty( gene_tree.getName() ) ? "" : gene_tree.getName() ) );
        log_writer.println( "Species tree file                        : " + species_tree_file.getCanonicalPath() );
        System.out.println( "Species tree file                        : " + species_tree_file.getCanonicalPath() );
        log_writer.println( "Species tree name                        : "
                + ( ForesterUtil.isEmpty( species_tree.getName() ) ? "" : gene_tree.getName() ) );
        System.out.println( "Species tree name                        : "
                + ( ForesterUtil.isEmpty( species_tree.getName() ) ? "" : gene_tree.getName() ) );
        SDI sdi = null;
        final long start_time = new Date().getTime();
        try {
            if ( base_algorithm == BASE_ALGORITHM.GSDI ) {
                System.out.println( "Use most parsimonous duplication model   : " + most_parsimonous_duplication_model );
                System.out.println( "Allow stripping of gene tree nodes       : " + allow_stripping_of_gene_tree );
                log_writer.println( "Use most parsimonous duplication model   : " + most_parsimonous_duplication_model );
                log_writer.println( "Allow stripping of gene tree nodes       : " + allow_stripping_of_gene_tree );
                log_writer.flush();
                sdi = new GSDI( gene_tree,
                                species_tree,
                                most_parsimonous_duplication_model,
                                allow_stripping_of_gene_tree,
                                true );
            }
            else {
                System.out.println();
                System.out.println( "Using SDIse algorithm" );
                log_writer.println( "Using SDIse algorithm" );
                log_writer.flush();
                sdi = new SDIse( gene_tree, species_tree );
            }
        }
        catch ( final SDIException e ) {
            fatalError( "user error", e.getLocalizedMessage(), log_writer );
        }
        catch ( final IOException e ) {
            fatalError( "error", e.toString(), log_writer );
        }
        catch ( final Exception e ) {
            e.printStackTrace();
            fatalError( "unexpected error", e.toString(), log_writer );
        }
        System.out.println( "Running time (excluding I/O)             : " + ( new Date().getTime() - start_time )
                + "ms" );
        log_writer.println( "Running time (excluding I/O)             : " + ( new Date().getTime() - start_time )
                + "ms" );
        if ( ( base_algorithm == BASE_ALGORITHM.GSDI ) ) {
            final GSDI gsdi = ( GSDI ) sdi;
            System.out.println( "Mapping based on                         : " + gsdi.getTaxCompBase() );
            log_writer.println( "Mapping based on                         : " + gsdi.getTaxCompBase() );
        }
        try {
            final PhylogenyWriter writer = new PhylogenyWriter();
            writer.toPhyloXML( out_file, gene_tree, 0 );
        }
        catch ( final IOException e ) {
            ForesterUtil.fatalError( PRG_NAME,
                                     "Failed to write to [" + out_file.getCanonicalPath() + "]: " + e.getMessage() );
        }
        System.out.println( "Wrote resulting gene tree to             : " + out_file.getCanonicalPath() );
        log_writer.println( "Wrote resulting gene tree to             : " + out_file.getCanonicalPath() );
        if ( base_algorithm == BASE_ALGORITHM.SDI ) {
            sdi.computeMappingCostL();
            System.out.println( "Mapping cost                             : " + sdi.computeMappingCostL() );
            log_writer.println( "Mapping cost                             : " + sdi.computeMappingCostL() );
        }
        else if ( ( base_algorithm == BASE_ALGORITHM.GSDI ) ) {
            final GSDI gsdi = ( GSDI ) sdi;
            final File species_tree_used_file = new File( ForesterUtil.removeSuffix( out_file.toString() )
                    + SUFFIX_FOR_SPECIES_TREE_USED );
            try {
                final PhylogenyWriter writer = new PhylogenyWriter();
                writer.toPhyloXML( species_tree_used_file, gsdi.getSpeciesTree(), 0 );
            }
            catch ( final IOException e ) {
                ForesterUtil.fatalError( PRG_NAME, "Failed to write to [" + species_tree_used_file.getCanonicalPath()
                        + "]: " + e.getMessage() );
            }
            System.out.println( "Wrote (stripped) species tree to         : "
                    + species_tree_used_file.getCanonicalPath() );
            log_writer.println( "Wrote (stripped) species tree to         : "
                    + species_tree_used_file.getCanonicalPath() );
            if ( ( gsdi.getReMappedScientificNamesFromGeneTree() != null )
                    && !gsdi.getReMappedScientificNamesFromGeneTree().isEmpty() ) {
                System.out.println( "Number of gene tree species remapped     : "
                        + gsdi.getReMappedScientificNamesFromGeneTree().size() );
                log_writer.println( "Number of gene tree species remapped     : "
                        + gsdi.getReMappedScientificNamesFromGeneTree().size() );
                writeToRemappedFile( out_file, gsdi.getReMappedScientificNamesFromGeneTree(), log_writer );
            }
        }
        System.out.println( "Number of external nodes in gene tree    : " + gene_tree.getNumberOfExternalNodes() );
        log_writer.println( "Number of external nodes in gene tree    : " + gene_tree.getNumberOfExternalNodes() );
        System.out.println( "Number of external nodes in species tree : "
                + sdi.getSpeciesTree().getNumberOfExternalNodes() );
        log_writer.println( "Number of external nodes in species tree : "
                + sdi.getSpeciesTree().getNumberOfExternalNodes() );
        if ( ( base_algorithm == BASE_ALGORITHM.GSDI ) ) {
            final GSDI gsdi = ( GSDI ) sdi;
            final int poly = PhylogenyMethods.countNumberOfPolytomies( gsdi.getSpeciesTree() );
            System.out.println( "Number of polytomies in species tree     : " + poly );
            log_writer.println( "Number of polytomies in species tree     : " + poly );
            System.out.println( "External nodes stripped from gene tree   : "
                    + gsdi.getStrippedExternalGeneTreeNodes().size() );
            log_writer.println( "External nodes stripped from gene tree   : "
                    + gsdi.getStrippedExternalGeneTreeNodes().size() );
            System.out.println( "External nodes stripped from species tree: "
                    + gsdi.getStrippedSpeciesTreeNodes().size() );
            log_writer.println( "External nodes stripped from species tree: "
                    + gsdi.getStrippedSpeciesTreeNodes().size() );
        }
        System.out.println();
        System.out.println( "Number of duplications                   : " + sdi.getDuplicationsSum() );
        log_writer.println( "Number of duplications                   : " + sdi.getDuplicationsSum() );
        if ( ( base_algorithm == BASE_ALGORITHM.GSDI ) ) {
            final GSDI gsdi = ( GSDI ) sdi;
            if ( !most_parsimonous_duplication_model ) {
                final int u = gsdi.getSpeciationOrDuplicationEventsSum();
                System.out.println( "Number of potential duplications         : " + u );
                log_writer.println( "Number of potential duplications         : " + u );
            }
            System.out.println( "Number of speciations                    : " + gsdi.getSpeciationsSum() );
            log_writer.println( "Number of speciations                    : " + gsdi.getSpeciationsSum() );
            log_writer.println();
            printMappedNodesToLog( log_writer, gsdi );
            log_writer.println();
            printStrippedGeneTreeNodesToLog( log_writer, gsdi );
        }
        System.out.println();
        System.out.println( "Wrote log to                             : " + log_file.getCanonicalPath() );
        System.out.println();
        log_writer.close();
    }

    private static void writeToRemappedFile( final File out_file,
                                             final SortedSet<String> remapped,
                                             final EasyWriter log_writer ) throws IOException {
        final File file = new File( ForesterUtil.removeSuffix( out_file.toString() ) + REMAPPED_SUFFIX );
        final EasyWriter remapped_writer = ForesterUtil.createEasyWriter( file );
        for( final String s : remapped ) {
            remapped_writer.println( s );
        }
        remapped_writer.close();
        System.out.println( "Wrote remapped gene tree species to      : " + file.getCanonicalPath() );
        log_writer.println( "Wrote remapped gene tree species to      : " + file.getCanonicalPath() );
    }

    private static void printMappedNodesToLog( final EasyWriter log_writer, final GSDI gsdi ) throws IOException {
        final SortedSet<String> ss = new TreeSet<String>();
        for( final PhylogenyNode n : gsdi.getMappedExternalSpeciesTreeNodes() ) {
            ss.add( n.toString() );
        }
        log_writer.println( "The following " + ss.size() + " species were used: " );
        for( final String s : ss ) {
            log_writer.println( "  " + s );
        }
    }

    private static void fatalError( final String type, final String msg, final EasyWriter log_writer ) {
        try {
            log_writer.flush();
            log_writer.println();
            log_writer.print( type.toUpperCase() + ": " );
            log_writer.println( msg );
            log_writer.close();
        }
        catch ( final IOException e ) {
            e.printStackTrace();
        }
        ForesterUtil.fatalError( gsdi.PRG_NAME, msg );
    }

    private static void printStrippedGeneTreeNodesToLog( final EasyWriter log_writer, final GSDI gsdi )
            throws IOException {
        final SortedMap<String, Integer> sm = new TreeMap<String, Integer>();
        for( final PhylogenyNode n : gsdi.getStrippedExternalGeneTreeNodes() ) {
            final String s = n.toString();
            if ( sm.containsKey( s ) ) {
                sm.put( s, sm.get( s ) + 1 );
            }
            else {
                sm.put( s, 1 );
            }
        }
        log_writer.println( "The following " + sm.size() + " nodes were stripped from the gene tree: " );
        for( final String s : sm.keySet() ) {
            final int count = sm.get( s );
            if ( count == 1 ) {
                log_writer.println( "  " + s );
            }
            else {
                log_writer.println( "  " + s + " [" + count + "]" );
            }
        }
    }

    private static void print_help() {
        System.out.println( "Usage: " + gsdi.PRG_NAME
                + " [-options] <gene tree in phyloXML format> <species tree> <outfile>" );
        System.out.println();
        System.out.println( "Options:" );
        System.out.println( " -" + gsdi.ALLOW_STRIPPING_OF_GENE_TREE_OPTION
                + ": to allow stripping of gene tree nodes without a matching species" );
        System.out.println( " -" + gsdi.MOST_PARSIMONIOUS_OPTION
                + ": use most parimonious duplication model for GSDI: " );
        System.out.println( "     assign nodes as speciations which would otherwise be assiged" );
        System.out.println( "     as potential duplications due to polytomies in the species tree" );
        System.out.println( " -" + gsdi.GUESS_FORMAT_OF_SPECIES_TREE
                + ": to allow species tree in other formats than phyloXML (i.e. Newick, NHX, Nexus)" );
        System.out.println( " -" + gsdi.SDISE_OPTION
                + ": to use SDIse algorithm instead of GSDI algorithm (for binary species trees)" );
        System.out.println();
        System.out.println( "Gene tree:" );
        System.out.println( " in phyloXM format, with taxonomy and sequence data in appropriate fields" );
        System.out.println();
        System.out.println( "Species tree:" );
        System.out.println( " in phyloXML format (unless option -" + gsdi.GUESS_FORMAT_OF_SPECIES_TREE + " is used)" );
        System.out.println();
        System.out.println( "Example: gsdi -" + ALLOW_STRIPPING_OF_GENE_TREE_OPTION
                + " gene_tree.xml tree_of_life.xml out.xml" );
        System.out.println();
    }
}
