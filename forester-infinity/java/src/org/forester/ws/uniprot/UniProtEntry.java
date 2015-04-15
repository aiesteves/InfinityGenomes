// $Id:
// forester -- software libraries and applications
// for genomics and evolutionary biology research.
//
// Copyright (C) 2010 Christian M Zmasek
// Copyright (C) 2010 Sanford-Burnham Medical Research Institute
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

package org.forester.ws.uniprot;

import java.util.List;

import org.forester.util.ForesterUtil;

public final class UniProtEntry implements SequenceDatabaseEntry {

    private String _ac;
    private String _rec_name;
    private String _os_scientific_name;
    private String _tax_id;
    private String _symbol;

    private UniProtEntry() {
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public static SequenceDatabaseEntry createInstanceFromPlainText( final List<String> lines ) {
        final UniProtEntry e = new UniProtEntry();
        for( final String line : lines ) {
            if ( line.startsWith( "AC" ) ) {
                e.setAc( DatabaseTools.extract( line, "AC", ";" ) );
            }
            else if ( line.startsWith( "DE" ) ) {
                if ( ( line.indexOf( "RecName:" ) > 0 ) && ( line.indexOf( "Full=" ) > 0 ) ) {
                    e.setRecName( DatabaseTools.extract( line, "Full=", ";" ) );
                }
            }
            else if ( line.startsWith( "GN" ) ) {
                if ( ( line.indexOf( "Name=" ) > 0 ) ) {
                    e.setSymbol( DatabaseTools.extract( line, "Name=", ";" ) );
                }
            }
            else if ( line.startsWith( "OS" ) ) {
                if ( line.indexOf( "(" ) > 0 ) {
                    e.setOsScientificName( DatabaseTools.extract( line, "OS", "(" ) );
                }
                else {
                    e.setOsScientificName( DatabaseTools.extract( line, "OS", "." ) );
                }
            }
            else if ( line.startsWith( "OX" ) ) {
                if ( line.indexOf( "NCBI_TaxID=" ) > 0 ) {
                    e.setTaxId( DatabaseTools.extract( line, "NCBI_TaxID=", ";" ) );
                }
            }
        }
        return e;
    }

    @Override
    public String getAccession() {
        return _ac;
    }

    private void setAc( final String ac ) {
        if ( _ac == null ) {
            _ac = ac;
        }
    }

    @Override
    public String getSequenceName() {
        return _rec_name;
    }

    private void setRecName( final String rec_name ) {
        if ( _rec_name == null ) {
            _rec_name = rec_name;
        }
    }

    @Override
    public String getTaxonomyScientificName() {
        return _os_scientific_name;
    }

    private void setOsScientificName( final String os_scientific_name ) {
        if ( _os_scientific_name == null ) {
            _os_scientific_name = os_scientific_name;
        }
    }

    @Override
    public String getTaxonomyIdentifier() {
        return _tax_id;
    }

    private void setTaxId( final String tax_id ) {
        if ( _tax_id == null ) {
            _tax_id = tax_id;
        }
    }

    @Override
    public String getSequenceSymbol() {
        return _symbol;
    }

    private void setSymbol( final String symbol ) {
        if ( _symbol == null ) {
            _symbol = symbol;
        }
    }

    @Override
    public boolean isEmpty() {
        return ( ForesterUtil.isEmpty( getAccession() ) && ForesterUtil.isEmpty( getSequenceName() )
                && ForesterUtil.isEmpty( getTaxonomyScientificName() )
                && ForesterUtil.isEmpty( getTaxonomyIdentifier() ) && ForesterUtil.isEmpty( getSequenceSymbol() ) );
    }

    @Override
    public String getProvider() {
        return "uniprot";
    }
}
