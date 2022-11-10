package mars.mips.dump;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import mars.Globals;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;

/**
 * Intel's Hex memory initialization format
 *
 * @author Leo Alterman
 * @version July 2011
 */

public class IntelHexDumpFormat extends AbstractDumpFormat {

	/**
	 * Constructor. File extention is "hex".
	 */
	public IntelHexDumpFormat() {
		super("Intel hex format", "HEX", "Written as Intel Hex Memory File", "hex");
	}

	/**
	 * Write MIPS memory contents according to the Memory Initialization File (MIF)
	 * specification.
	 *
	 * @param file         File in which to store MIPS memory contents.
	 * @param firstAddress first (lowest) memory address to dump. In bytes but must
	 *                     be on word boundary.
	 * @param lastAddress  last (highest) memory address to dump. In bytes but must
	 *                     be on word boundary. Will dump the word that starts at
	 *                     this address.
	 * @throws AddressErrorException if firstAddress is invalid or not on a word
	 *                               boundary.
	 * @throws IOException           if error occurs during file output.
	 */
	@Override
	public void dumpMemoryRange(final File file, final int firstAddress, final int lastAddress)
			throws AddressErrorException, IOException {
		final PrintStream out = new PrintStream(new FileOutputStream(file));
		String string = null;
		try {
			for (int address = firstAddress; address <= lastAddress; address += Memory.WORD_LENGTH_BYTES) {
				final Integer temp = Globals.memory.getRawWordOrNull(address);
				if (temp == null) { break; }
				string = Integer.toHexString(temp);
				while (string.length() < 8) {
					string = '0' + string;
				}
				String addr = Integer.toHexString(address - firstAddress);
				while (addr.length() < 4) {
					addr = '0' + addr;
				}
				String chksum;
				int tmp_chksum = 0;
				tmp_chksum += 4;
				tmp_chksum += 0xFF & address - firstAddress;
				tmp_chksum += 0xFF & address - firstAddress >> 8;
				tmp_chksum += 0xFF & temp.intValue();
				tmp_chksum += 0xFF & temp.intValue() >> 8;
				tmp_chksum += 0xFF & temp.intValue() >> 16;
				tmp_chksum += 0xFF & temp.intValue() >> 24;
				tmp_chksum = tmp_chksum % 256;
				tmp_chksum = ~tmp_chksum + 1;
				chksum = Integer.toHexString(0xFF & tmp_chksum);
				if (chksum.length() == 1) { chksum = '0' + chksum; }
				final String finalstr = ":04" + addr + "00" + string + chksum;
				out.println(finalstr.toUpperCase());
			}
			out.println(":00000001FF");
		} finally {
			out.close();
		}

	}
}
