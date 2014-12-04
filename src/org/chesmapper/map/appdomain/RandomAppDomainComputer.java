package org.chesmapper.map.appdomain;

import java.util.Random;

public class RandomAppDomainComputer extends AbstractAppDomainComputer
{
	public static RandomAppDomainComputer INSTANCE = new RandomAppDomainComputer();

	private RandomAppDomainComputer()
	{
	}

	@Override
	public void computeAppDomain()
	{
		//		this.dataset = dataset;
		Random r = new Random();
		for (int i = 0; i < compounds.size(); i++)
		{
			pValues[i] = r.nextDouble();
			inside[i] = pValues[i] >= 0.5;
		}
	}

	@Override
	public String getShortName()
	{
		return "random";
	}

}
