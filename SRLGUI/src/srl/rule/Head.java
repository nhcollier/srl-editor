/* 
 * Copyright (c) 2008, National Institute of Informatics
 *
 * This file is part of SRL, and is free
 * software, licenced under the GNU Library General Public License,
 * Version 2, June 1991.
 *
 * A copy of this licence is included in the distribution in the file
 * licence.html, and is also available at http://www.fsf.org/licensing/licenses/info/GPLv2.html.
 */
package srl.rule;

/**
 *
 * @author john
 */
public class Head {

    public final String name, var;
    
    public Head(String name, String var) {
        this.name = name;
        this.var = var;
    }

    @Override
    public String toString() {
        return name + "(" + var + ")";
    }
}
