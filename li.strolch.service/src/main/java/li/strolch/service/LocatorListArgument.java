package li.strolch.service;

import java.util.List;

import li.strolch.model.Locator;
import li.strolch.service.api.ServiceArgument;

public class LocatorListArgument extends ServiceArgument {
	private static final long serialVersionUID = 1L;
	public List<Locator> locators;
}
