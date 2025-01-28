package test.kar.archidata.checker.model;

import org.kar.archidata.dataAccess.options.CheckJPA;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class JpaBaseModel {
	// Simple checker declaration
	public static class JpaBaseModelChecker extends CheckJPA<JpaBaseModel> {
		public JpaBaseModelChecker() {
			super(JpaBaseModel.class);
		}
	}

	// Simple data to verify if the checker is active
	@Size(min = 3, max = 8)
	public String testSize;

	@Pattern(regexp = "^[0-9]+$")
	public String testPattern;

	@Email
	public String testEMail;

	@Min(-75)
	public int testMinInteger;
	@Min(-75)
	public Integer testMinIntegerObject;
	@Min(-75)
	public long testMinLong;
	@Min(-75)
	public Long testMinLongObject;
	@Min(-75)
	public float testMinFloat;
	@Min(-75)
	public Float testMinFloatObject;
	@Min(-75)
	public double testMinDouble;
	@Min(-75)
	public Double testMinDoubleObject;

	@Max(75)
	public int testMaxInteger;
	@Max(75)
	public Integer testMaxIntegerObject;
	@Max(75)
	public long testMaxLong;
	@Max(75)
	public Long testMaxLongObject;
	@Max(75)
	public float testMaxFloat;
	@Max(75)
	public Float testMaxFloatObject;
	@Max(75)
	public double testMaxDouble;
	@Max(75)
	public Double testMaxDoubleObject;

	@DecimalMin("-75")
	public int testDecimalMinIncludeInteger;
	@DecimalMin("-75")
	public Integer testDecimalMinIncludeIntegerObject;
	@DecimalMin("-75")
	public long testDecimalMinIncludeLong;
	@DecimalMin("-75")
	public Long testDecimalMinIncludeLongObject;
	@DecimalMin("-75.56")
	public float testDecimalMinIncludeFloat;
	@DecimalMin("-75.56")
	public Float testDecimalMinIncludeFloatObject;
	@DecimalMin("-75.56")
	public double testDecimalMinIncludeDouble;
	@DecimalMin("-75.56")
	public Double testDecimalMinIncludeDoubleObject;

	@DecimalMax("75")
	public int testDecimalMaxIncludeInteger;
	@DecimalMax("75")
	public Integer testDecimalMaxIncludeIntegerObject;
	@DecimalMax("75")
	public long testDecimalMaxIncludeLong;
	@DecimalMax("75")
	public Long testDecimalMaxIncludeLongObject;
	@DecimalMax("75.56")
	public float testDecimalMaxIncludeFloat;
	@DecimalMax("75.56")
	public Float testDecimalMaxIncludeFloatObject;
	@DecimalMax("75.56")
	public double testDecimalMaxIncludeDouble;
	@DecimalMax("75.56")
	public Double testDecimalMaxIncludeDoubleObject;

	@DecimalMin(value = "-75", inclusive = false)
	public int testDecimalMinExcludeInteger;
	@DecimalMin(value = "-75", inclusive = false)
	public Integer testDecimalMinExcludeIntegerObject;
	@DecimalMin(value = "-75", inclusive = false)
	public long testDecimalMinExcludeLong;
	@DecimalMin(value = "-75", inclusive = false)
	public Long testDecimalMinExcludeLongObject;
	@DecimalMin(value = "-75.56", inclusive = false)
	public float testDecimalMinExcludeFloat;
	@DecimalMin(value = "-75.56", inclusive = false)
	public Float testDecimalMinExcludeFloatObject;
	@DecimalMin(value = "-75.56", inclusive = false)
	public double testDecimalMinExcludeDouble;
	@DecimalMin(value = "-75.56", inclusive = false)
	public Double testDecimalMinExcludeDoubleObject;

	@DecimalMax(value = "75", inclusive = false)
	public int testDecimalMaxExcludeInteger;
	@DecimalMax(value = "75", inclusive = false)
	public Integer testDecimalMaxExcludeIntegerObject;
	@DecimalMax(value = "75", inclusive = false)
	public long testDecimalMaxExcludeLong;
	@DecimalMax(value = "75", inclusive = false)
	public Long testDecimalMaxExcludeLongObject;
	@DecimalMax(value = "75.56", inclusive = false)
	public float testDecimalMaxExcludeFloat;
	@DecimalMax(value = "75.56", inclusive = false)
	public Float testDecimalMaxExcludeFloatObject;
	@DecimalMax(value = "75.56", inclusive = false)
	public double testDecimalMaxExcludeDouble;
	@DecimalMax(value = "75.56", inclusive = false)
	public Double testDecimalMaxExcludeDoubleObject;
}
