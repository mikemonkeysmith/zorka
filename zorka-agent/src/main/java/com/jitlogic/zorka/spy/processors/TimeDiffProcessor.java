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

package com.jitlogic.zorka.spy.processors;

import com.jitlogic.zorka.spy.SpyProcessor;
import com.jitlogic.zorka.spy.SpyRecord;
import static com.jitlogic.zorka.spy.SpyLib.fs;

public class TimeDiffProcessor implements SpyProcessor {

    private int itStart, stStart, itStop, stStop, iRslt, sRslt;


    public TimeDiffProcessor(int[] tStart, int[] tStop, int[] rslt) {
        this.stStart = tStart[0]; this.itStart = tStart[1];
        this.stStop = tStop[0]; this.itStop = tStop[1];
        this.sRslt = rslt[0]; this.iRslt = rslt[1];
    }


    public SpyRecord process(int stage, SpyRecord record) {
        Object  v1 = record.get(fs(stStart, stage), itStart),
                v2 = record.get(fs(stStop, stage), itStop);

        if (v1 instanceof Long && v2 instanceof Long) {
            long l1 = (Long)v1, l2 = (Long)v2;
            record.put(fs(sRslt, stage), iRslt, l2-l1);
        } // TODO else (log something here ?)

        return record;
    }

    // TODO get rid of this

    public int getStartSlot() {
        return itStart;
    }


    public int getStopSlot() {
        return itStop;
    }


    public int getResultSlot() {
        return iRslt;
    }
}
