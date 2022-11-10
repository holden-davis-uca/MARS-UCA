package mars.mips.instructions.syscalls;

/**
 * Represents User override of default syscall number assignment. Such overrides
 * are specified in the config.txt file read when MARS starts up.
 */

public class SyscallNumberOverride {

	private final String serviceName;
	private int newServiceNumber;

	/**
	 * Constructor is called with two strings: service name and desired number. Will
	 * throw an exception is number is malformed, but does not check validity of the
	 * service name or number.
	 *
	 * @param serviceName a String containing syscall service mnemonic.
	 * @param value       a String containing its reassigned syscall service number.
	 *                    If this number is previously assigned to a different
	 *                    syscall which does not also receive a new number, then an
	 *                    error for duplicate numbers will be issued at MARS launch.
	 */

	public SyscallNumberOverride(final String serviceName, final String value) {
		this.serviceName = serviceName;
		try {
			newServiceNumber = Integer.parseInt(value.trim());
		} catch (final NumberFormatException e) {
			System.out.println("Error processing Syscall number override: '" + value.trim()
					+ "' is not a valid integer");
			System.exit(0);
		}
	}

	/**
	 * Get the service name as a String.
	 *
	 * @return the service name
	 */
	public String getName() { return serviceName; }

	/**
	 * Get the new service number as an int.
	 *
	 * @return the service number
	 */
	public int getNumber() { return newServiceNumber; }

}
