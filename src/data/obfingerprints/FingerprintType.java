package data.obfingerprints;

public enum FingerprintType
{
	FP2, FP3, FP4, MACCS;

	public static FingerprintType[] visible_values()
	{
		return new FingerprintType[] { FP2 };
		//		return new FingerprintType[] { FP2, FP3, FP4, MACCS };
	}

	public static FingerprintType[] hidden_values()
	{
		return new FingerprintType[] { FP3, FP4, MACCS };
		//return new FingerprintType[] {};
	}
}
