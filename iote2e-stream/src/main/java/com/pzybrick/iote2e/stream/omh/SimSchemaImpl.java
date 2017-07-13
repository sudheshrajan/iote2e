package com.pzybrick.iote2e.stream.omh;

import static java.math.BigDecimal.ONE;
import static org.openmhealth.schema.domain.omh.BloodGlucoseUnit.MILLIGRAMS_PER_DECILITER;
import static org.openmhealth.schema.domain.omh.BloodSpecimenType.WHOLE_BLOOD;
import static org.openmhealth.schema.domain.omh.DescriptiveStatistic.MEDIAN;
import static org.openmhealth.schema.domain.omh.TemperatureUnit.FAHRENHEIT;
import static org.openmhealth.schema.domain.omh.TemporalRelationshipToMeal.FASTING;
import static org.openmhealth.schema.domain.omh.TemporalRelationshipToSleep.BEFORE_SLEEPING;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Random;

import org.openmhealth.schema.domain.omh.AmbientTemperature;
import org.openmhealth.schema.domain.omh.BloodGlucose;
import org.openmhealth.schema.domain.omh.BloodGlucoseUnit;
import org.openmhealth.schema.domain.omh.BloodPressure;
import org.openmhealth.schema.domain.omh.BloodPressureUnit;
import org.openmhealth.schema.domain.omh.DiastolicBloodPressure;
import org.openmhealth.schema.domain.omh.HeartRate;
import org.openmhealth.schema.domain.omh.HeartRateUnit;
import org.openmhealth.schema.domain.omh.KcalUnit;
import org.openmhealth.schema.domain.omh.KcalUnitValue;
import org.openmhealth.schema.domain.omh.LengthUnit;
import org.openmhealth.schema.domain.omh.LengthUnitValue;
import org.openmhealth.schema.domain.omh.PhysicalActivity;
import org.openmhealth.schema.domain.omh.PositionDuringMeasurement;
import org.openmhealth.schema.domain.omh.SchemaId;
import org.openmhealth.schema.domain.omh.SchemaSupport;
import org.openmhealth.schema.domain.omh.SystolicBloodPressure;
import org.openmhealth.schema.domain.omh.TemperatureUnitValue;
import org.openmhealth.schema.domain.omh.TemporalRelationshipToPhysicalActivity;
import org.openmhealth.schema.domain.omh.TimeFrame;
import org.openmhealth.schema.domain.omh.TypedUnitValue;

public class SimSchemaImpl {
	private static Random random = new Random();
	
	public static class SimSchemaBloodGlucoseImpl implements SimSchema {
		@Override
		public SchemaId getSchemaId() {
			return BloodGlucose.SCHEMA_ID;
		}
		
		@Override
		public Object createBody( OffsetDateTime now, Object prevBody ) throws Exception {
			final long mid = 110;
			final long max = 130;
			final long min = 90;
			final long exceed = 170;
			final long incr = 3;
			long value = 0;
			if( prevBody != null ) {
				long prevValue = ((BloodGlucose)prevBody).getBloodGlucose().getValue().longValue();
				if( (random.nextInt() % 2) == 1 ) value = prevValue + incr;
				else value = prevValue - incr;
				if( value < min ) value = min;
				else if( value > max ) value = max;
			} else value = mid;
			// 5% of the time use exceeded value
			String userNotes = "Feeling fine";
			if( random.nextInt(20) == 0 ) {
				value = exceed;
				userNotes = "light headed";
			}
			
	        TypedUnitValue<BloodGlucoseUnit> bloodGlucoseLevel = new TypedUnitValue<>(MILLIGRAMS_PER_DECILITER, value);
	        BloodGlucose bloodGlucose = new BloodGlucose.Builder(bloodGlucoseLevel)
	                .setBloodSpecimenType(WHOLE_BLOOD)
	                .setTemporalRelationshipToMeal(FASTING)
	                .setTemporalRelationshipToSleep(BEFORE_SLEEPING)
	                .setEffectiveTimeFrame( new TimeFrame(now) )
	                // .setEffectiveTimeFrame()
	                .setDescriptiveStatistic(MEDIAN)
	                .setUserNotes(userNotes)
	                .build();
	        return bloodGlucose;
		}
		
	}
	
	public static class SimSchemaAmbientTempImpl implements SimSchema {
		@Override
		public SchemaId getSchemaId() {
			// for some reason SCHEMA_ID is private for ambient-temperature
			return new SchemaId(SchemaSupport.OMH_NAMESPACE, "ambient-temperature", "1.0");
		}
		
		@Override
		public Object createBody( OffsetDateTime now, Object prevBody ) throws Exception {
			final long mid = 80;
			final long max = 90;
			final long min = 70;
			final long exceed = 105;
			final long incr = 2;
			long value = 0;
			if( prevBody != null ) {
				long prevValue = ((AmbientTemperature)prevBody).getAmbientTemperature().getValue().longValue();
				if( (random.nextInt() % 2) == 1 ) value = prevValue + incr;
				else value = prevValue - incr;
				if( value < min ) value = min;
				else if( value > max ) value = max;
			} else value = mid;
			// 5% of the time use exceeded value
			String userNotes = "Feeling fine";
			if( random.nextInt(20) == 0 ) {
				value = exceed;
				userNotes = "over heating";
			}
	        AmbientTemperature ambientTemperature =
	                new AmbientTemperature.Builder(new TemperatureUnitValue(FAHRENHEIT, value))
	                        .setDescriptiveStatistic(MEDIAN)
	                        .setEffectiveTimeFrame(new TimeFrame(now))
	                        .setUserNotes(userNotes)
	                        .build();
	        return ambientTemperature;
		}
	}
	
	
	public static class SimSchemaBloodPressureImpl implements SimSchema {
		@Override
		public SchemaId getSchemaId() {
			return BloodPressure.SCHEMA_ID;
		}
		
		@Override
		public Object createBody( OffsetDateTime now, Object prevBody ) throws Exception {
			// Systolic
			final long midSys = 80;
			final long maxSys = 90;
			final long minSys = 70;
			final long exceedSys = 105;
			final long incrSys = 2;
			long valueSys = 0;
			if( prevBody != null ) {
				long prevValueSys = ((BloodPressure)prevBody).getSystolicBloodPressure().getValue().longValue();
				if( (random.nextInt() % 2) == 1 ) valueSys = prevValueSys + incrSys;
				else valueSys = prevValueSys - incrSys;
				if( valueSys < minSys ) valueSys = minSys;
				else if( valueSys > maxSys ) valueSys = maxSys;
			} else valueSys = midSys;
			// 5% of the time use exceeded value
			String userNotes = "Feeling fine";
			if( random.nextInt(20) == 0 ) {
				valueSys = exceedSys;
				userNotes = "dizzy";
			}
			
			// Diastolic
			final long midDia = 80;
			final long maxDia = 90;
			final long minDia = 70;
			final long exceedDia = 105;
			final long incrDia = 2;
			long valueDia = 0;
			if( prevBody != null ) {
				long prevValueDia = ((BloodPressure)prevBody).getDiastolicBloodPressure().getValue().longValue();
				if( (random.nextInt() % 2) == 1 ) valueDia = prevValueDia + incrDia;
				else valueDia = prevValueDia - incrDia;
				if( valueDia < minDia ) valueDia = minDia;
				else if( valueDia > maxDia ) valueDia = maxDia;
			} else valueDia = midDia;
			// 5% of the time use exceeded value
			if( random.nextInt(20) == 0 ) {
				valueDia = exceedDia;
				userNotes = "dizzy";
			}
			
			SystolicBloodPressure systolicBloodPressure = new SystolicBloodPressure(BloodPressureUnit.MM_OF_MERCURY, BigDecimal.valueOf(valueSys));
			DiastolicBloodPressure diastolicBloodPressure = new DiastolicBloodPressure(BloodPressureUnit.MM_OF_MERCURY, BigDecimal.valueOf(valueDia));			
	        BloodPressure bloodPressure = new BloodPressure.Builder(systolicBloodPressure, diastolicBloodPressure)
	                .setPositionDuringMeasurement(PositionDuringMeasurement.SITTING)
	                .setEffectiveTimeFrame(new TimeFrame(now))
	                .setDescriptiveStatistic(MEDIAN)
	                .setUserNotes(userNotes)
	                .build();
	        return bloodPressure;
		}
		
	}

	
	public static class SimSchemaHeartRateImpl implements SimSchema {
		@Override
		public SchemaId getSchemaId() {
			return HeartRate.SCHEMA_ID;
		}
		
		@Override
		public Object createBody( OffsetDateTime now, Object prevBody ) throws Exception {
			final long mid = 72;
			final long max = 85;
			final long min = 65;
			final long exceed = 100;
			final long incr = 3;
			long value = 0;
			if( prevBody != null ) {
				long prevValue = ((HeartRate)prevBody).getHeartRate().getValue().longValue();
				if( (random.nextInt() % 2) == 1 ) value = prevValue + incr;
				else value = prevValue - incr;
				if( value < min ) value = min;
				else if( value > max ) value = max;
			} else value = mid;
			// 5% of the time use exceeded value
			String userNotes = "Feeling fine";
			if( random.nextInt(20) == 0 ) {
				value = exceed;
				userNotes = "dizzy";
			}
			
			TypedUnitValue<HeartRateUnit> heartRateUnit = new TypedUnitValue<>(HeartRateUnit.BEATS_PER_MINUTE, value);
	        HeartRate heartRate = new HeartRate.Builder(heartRateUnit)
	        		.setEffectiveTimeFrame(new TimeFrame(now))
	                .setTemporalRelationshipToPhysicalActivity(TemporalRelationshipToPhysicalActivity.AT_REST)
	                .setUserNotes(userNotes)
	                .build();

	        return heartRate;
		}
		
	}

	
	public static class SimSchemaHkWorkoutImpl implements SimSchema {
		@Override
		public SchemaId getSchemaId() {
			return PhysicalActivity.SCHEMA_ID;
		}
		
		@Override
		public Object createBody( OffsetDateTime now, Object prevBody ) throws Exception {
			// Distance
			final long midDistance = 15;
			final long maxDistance = 25;
			final long minDistance = 5;
			final long exceedDistance = 50;
			final long incrDistance = 5;
			long valueDistance = 0;
			if( prevBody != null ) {
				long prevValueDistance = ((PhysicalActivity)prevBody).getDistance().getValue().longValue();
				if( (random.nextInt() % 2) == 1 ) valueDistance = prevValueDistance + incrDistance;
				else valueDistance = prevValueDistance - incrDistance;
				if( valueDistance < minDistance ) valueDistance = minDistance;
				else if( valueDistance > maxDistance ) valueDistance = maxDistance;
			} else valueDistance = midDistance;
			// 5% of the time use exceeded value
			if( random.nextInt(20) == 0 ) {
				valueDistance = exceedDistance;
			}
			
			// KCals
			final long midKcals = 500;
			final long maxKcals = 1050;
			final long minKcals = 250;
			final long exceedKcals = 2000;
			final long incrKcals = 100;
			long valueKcals = 0;
			if( prevBody != null ) {
				long prevValueKcals = ((PhysicalActivity)prevBody).getCaloriesBurned().getValue().longValue();
				if( (random.nextInt() % 2) == 1 ) valueKcals = prevValueKcals + incrKcals;
				else valueKcals = prevValueKcals - incrKcals;
				if( valueKcals < minKcals ) valueKcals = minKcals;
				else if( valueKcals > maxKcals ) valueKcals = maxKcals;
			} else valueKcals = midKcals;
			// 5% of the time use exceeded value
			if( random.nextInt(20) == 0 ) {
				valueKcals = exceedKcals;
			}

	        LengthUnitValue distance = new LengthUnitValue(LengthUnit.MILE, new BigDecimal(valueDistance));
	        KcalUnitValue caloriesBurned = new KcalUnitValue(KcalUnit.KILOCALORIE, new BigDecimal(valueKcals));
	        TimeFrame timeFrame = new TimeFrame(OffsetDateTime.now() );
	        PhysicalActivity physicalActivity = new PhysicalActivity.Builder("HKWorkoutActivityTypeCycling")
	                .setDistance(distance)
	                .setEffectiveTimeFrame(timeFrame)
	                .setCaloriesBurned(caloriesBurned)
	                .build();

	        return physicalActivity;
		}
		
	}
}