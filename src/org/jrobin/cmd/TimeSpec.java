/* ============================================================
 * JRobin : Pure java implementation of RRDTool's functionality
 * ============================================================
 *
 * Project Info:  http://www.jrobin.org
 * Project Lead:  Sasa Markovic (saxon@jrobin.org);
 *
 * (C) Copyright 2003, by Sasa Markovic.
 *
 * Developers:    Sasa Markovic (saxon@jrobin.org)
 *                Arne Vandamme (cobralord@jrobin.org)
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package org.jrobin.cmd;

import org.jrobin.core.RrdException;
import org.jrobin.core.Util;

import java.util.GregorianCalendar;
import java.util.Date;
import java.util.Calendar;

class TimeSpec {
	public static final int TYPE_ABSOLUTE = 0;
	public static final int TYPE_START = 1;
	public static final int TYPE_END = 2;

	int type = TYPE_ABSOLUTE;
	int year, month, day, hour, min, sec;
	int wday;
	int dyear, dmonth, dday, dhour, dmin, dsec;

	String dateString;

	TimeSpec context;

	public TimeSpec(String dateString) {
		this.dateString = dateString;
	}

	void localtime(long timestamp) {
		GregorianCalendar date = new GregorianCalendar();
		date.setTime(new Date(timestamp * 1000L));
		year = date.get(Calendar.YEAR) - 1900;
		month = date.get(Calendar.MONTH);
		day = date.get(Calendar.DAY_OF_MONTH);
		hour = date.get(Calendar.HOUR_OF_DAY);
		min = date.get(Calendar.MINUTE);
		sec = date.get(Calendar.SECOND);
		wday = date.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
	}

	GregorianCalendar getTime() throws RrdException {
		GregorianCalendar gc;
		// absoulte time, this is easy
		if(type ==  TYPE_ABSOLUTE) {
			gc = new GregorianCalendar(year + 1900, month, day, hour, min, sec);
		}
		// relative time, we need a context to evaluate it
		else if(context != null && context.type == TYPE_ABSOLUTE) {
			gc = context.getTime();
		}
		// how would I guess what time it was?
		else {
			throw new RrdException("Relative times like '" +
					dateString + "' require proper absolute context to be evaluated");
		}
		gc.add(Calendar.YEAR, dyear);
		gc.add(Calendar.MONTH, dmonth);
		gc.add(Calendar.DAY_OF_MONTH, dday);
		gc.add(Calendar.HOUR_OF_DAY, dhour);
		gc.add(Calendar.MINUTE, dmin);
		gc.add(Calendar.SECOND, dsec);
		return gc;
	}

	long getTimestamp() throws RrdException {
		return Util.getTimestamp(getTime());
	}

	String dump() {
		return (type == TYPE_ABSOLUTE? "ABSTIME": type == TYPE_START? "START": "END") +
				": " + year + "/" + month + "/" + day +
				"/" + hour + "/" + min + "/" + sec + " (" +
				dyear + "/" + dmonth + "/" + dday +
				"/" + dhour + "/" + dmin + "/" + dsec + ")";
	}

	static GregorianCalendar[] getTimes(TimeSpec spec1, TimeSpec spec2) throws RrdException {
		if(spec1.type == TYPE_START || spec2.type == TYPE_END) {
			throw new RrdException("Recursive time specifications not allowed");
		}
		spec1.context = spec2;
		spec2.context = spec1;
		return new GregorianCalendar[] {
			spec1.getTime(),
			spec2.getTime()
		};
	}

	static long[] getTimestamps(TimeSpec spec1, TimeSpec spec2) throws RrdException {
		GregorianCalendar[] gcs = getTimes(spec1, spec2);
		return new long[] {
			Util.getTimestamp(gcs[0]), Util.getTimestamp(gcs[1])
		};
	}
}

