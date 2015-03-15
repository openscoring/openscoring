/*
 * Copyright (c) 2015 Villu Ruusmann
 *
 * This file is part of Openscoring
 *
 * Openscoring is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Openscoring is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Openscoring.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openscoring.common;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

@JsonInclude (
	value = JsonInclude.Include.NON_EMPTY
)
public class MetricSetResponse extends SimpleResponse {

	private String version = null;

	private Map<String, Map<String, Object>> counters = null;

	private Map<String, Map<String, Object>> gauges = null;

	private Map<String, Map<String, Object>> histograms = null;

	private Map<String, Map<String, Object>> meters = null;

	private Map<String, Map<String, Object>> timers = null;


	@Override
	public String toString(){
		String message = getMessage();
		if(message != null){
			return super.toString();
		}

		ToStringHelper stringHelper = MoreObjects.toStringHelper(getClass())
			.add("version", getVersion())
			.add("counters", getCounters())
			.add("gauges", getGauges())
			.add("histograms", getHistograms())
			.add("meters", getMeters())
			.add("timers", getTimers());

		return stringHelper.toString();
	}

	public String getVersion(){
		return this.version;
	}

	public void setVersion(String version){
		this.version = version;
	}

	public Map<String, Map<String, Object>> getCounters(){
		return this.counters;
	}

	public void setCounters(Map<String, Map<String, Object>> counters){
		this.counters = counters;
	}

	public Map<String, Map<String, Object>> getGauges(){
		return this.gauges;
	}

	public void setGauges(Map<String, Map<String, Object>> gauges){
		this.gauges = gauges;
	}

	public Map<String, Map<String, Object>> getHistograms(){
		return this.histograms;
	}

	public void setHistograms(Map<String, Map<String, Object>> histograms){
		this.histograms = histograms;
	}

	public Map<String, Map<String, Object>> getMeters(){
		return this.meters;
	}

	public void setMeters(Map<String, Map<String, Object>> meters){
		this.meters = meters;
	}

	public Map<String, Map<String, Object>> getTimers(){
		return this.timers;
	}

	public void setTimers(Map<String, Map<String, Object>> timers){
		this.timers = timers;
	}
}