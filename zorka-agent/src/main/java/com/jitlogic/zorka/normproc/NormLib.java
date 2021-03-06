/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.normproc;

import static com.jitlogic.zorka.normproc.XqlLexer.*;

public class NormLib {

    /**
     * Normalize whitespaces, symbols and keywords. Remove comments and unknown tokens, leave literals.
     */
    public final static int NORM_MIN =
            (1<<T_UNKNOWN)|(1<<T_WHITESPACE)|(1<<T_SYMBOL)|(1<<T_COMMENT)|(1<<T_KEYWORD);

    /**
     * Normalize whitespaces, symbols and keywords. Remove comments and unknown tokens, replace literals with placeholders.
     */
    public final static int NORM_STD =
            (1<<T_UNKNOWN)|(1<<T_WHITESPACE)|(1<<T_SYMBOL)|(1<<T_LITERAL)|(1<<T_COMMENT)|(1<<T_KEYWORD);

    /**
     * Supported SQL dialects.
     */
    public final static int DIALECT_SQL_92 = 0;
    public final static int DIALECT_SQL_99 = 1;
    public final static int DIALECT_SQL_03 = 2;
    public final static int DIALECT_MSSQL  = 3;
    public final static int DIALECT_PGSQL  = 4;
    public final static int DIALECT_MYSQL  = 5;
    public final static int DIALECT_DB2    = 6;
    public final static int DIALECT_ORACLE = 7;
    public final static int DIALECT_HQL    = 8;
    public final static int DIALECT_JPA    = 9;


    public Normalizer sql(int dialect, int flags) {
        return GenericNormalizer.xql(dialect, flags);
    }

    public Normalizer ldap(int flags) {
        return GenericNormalizer.ldap(flags);
    }
}
