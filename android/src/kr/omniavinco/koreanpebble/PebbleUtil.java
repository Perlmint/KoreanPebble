package kr.omniavinco.koreanpebble;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class PebbleUtil {
	final static Byte first_set[][] = {{(byte)233},{(byte)233,(byte)233},{(byte)230},{(byte)243},{(byte)243,(byte)243},{(byte)247},{(byte)231},{(byte)185},{(byte)185,(byte)185},{(byte)236},{(byte)236,(byte)236},{(byte)232},{(byte)234},{(byte)234,(byte)234},{(byte)237},{(byte)174},{(byte)165},{(byte)238},{(byte)235}};
	final static Byte middle_set[][] = {{(byte)228},{(byte)240},{(byte)180},{(byte)197},{(byte)242},{(byte)225},{(byte)227},{(byte)181},{(byte)244},{(byte)244,(byte)228},{(byte)244,(byte)240},{(byte)244,(byte)223},{(byte)178},{(byte)224},{(byte)224,(byte)242},{(byte)224,(byte)225},{(byte)224,(byte)226},{(byte)179},{(byte)229},{(byte)182},{(byte)226}};
	final static Byte last_set[][] = {{},{(byte)246},{(byte)159},{(byte)244},{(byte)241},{(byte)195},{(byte)209},{(byte)191},{(byte)245},{(byte)190},{(byte)228},{(byte)194},{(byte)210},{(byte)163},{(byte)162},{(byte)208},{(byte)248},{(byte)177},{(byte)214},{(byte)239},{(byte)176},{(byte)223},{(byte)161},{(byte)216},{(byte)193},{(byte)213},{(byte)207},{(byte)175}};
	final static Byte single_set[][] = {{(byte)0xE9}, {(byte)233,(byte)233},{(byte)233,(byte)236},{(byte)230},{(byte)230,(byte)234},{(byte)230,(byte)235},{(byte)243},{(byte)243,(byte)243},{(byte)247},{(byte)247,(byte)233},{(byte)247,(byte)231},{(byte)247,(byte)185},{(byte)247,(byte)236},{(byte)247,(byte)165},{(byte)247,(byte)238},{(byte)247,(byte)235},{(byte)231},{(byte)185},{(byte)185,(byte)185},{(byte)185,(byte)236},{(byte)236},{(byte)236,(byte)236},{(byte)232},{(byte)234},{(byte)234,(byte)234},{(byte)237},{(byte)174},{(byte)165},{(byte)238},{(byte)235},{(byte)228},{(byte)240},{(byte)180},{(byte)197},{(byte)242},{(byte)225},{(byte)227},{(byte)181},{(byte)225},{(byte)244,(byte)228},{(byte)244,(byte)240},{(byte)244,(byte)223},{(byte)178},{(byte)224},{(byte)224,(byte)242},{(byte)224,(byte)225},{(byte)224,(byte)226},{(byte)179},{(byte)229},{(byte)182},{(byte)226}};

	public static String join(final Iterable<?> iterable, String separator) {

        // handle null, zero and one elements before building a buffer
        if (iterable == null) {
            return null;
        }
        Iterator<?> iterator = iterable.iterator();
        if (!iterator.hasNext()) {
            return "";
        }
        final Object first = iterator.next();
        if (!iterator.hasNext()) {
            return first.toString();
        }

        // two or more elements
        final StringBuilder buf = new StringBuilder(256); // Java default is 16, probably too small
        if (first != null) {
            buf.append(first);
        }

        while (iterator.hasNext()) {
            buf.append(separator);
            final Object obj = iterator.next();
            if (obj != null) {
                buf.append(obj);
            }
        }

        return buf.toString();
    }
	
	public static <T> T[] concatAll(T[] first, T[]... rest) {
	  int totalLength = first.length;
	  for (T[] array : rest) {
	    totalLength += array.length;
	  }
	  T[] result = Arrays.copyOf(first, totalLength);
	  int offset = first.length;
	  for (T[] array : rest) {
	    System.arraycopy(array, 0, result, offset, array.length);
	    offset += array.length;
	  }
	  return result;
	}
	
	static List<Byte> unicode2han3last_direct(Character c) {
		Integer code = (int) c.charValue();
		if (code >= 44032 && code <= 55203) { // Korean Area
			code -= 44032;
			Integer last = code % 28;
			code = (code / 28);
			Integer middle = code % 21;
			Integer first = code / 21;
			return Arrays.asList(concatAll(first_set[first], middle_set[middle], last_set[last]));
		}
		if (code >= 12593 && code <= 12643) { // child sound
			return Arrays.asList(single_set[code - 12593]);
		}
		return Arrays.asList(code.byteValue());
	}
	static List<Byte> unicode2han_str(String str) {
		List<Byte> result = new ArrayList<Byte>();
		char[] chars = str.toCharArray();
		for(Character c : chars) {
			result.addAll(unicode2han3last_direct(c));
		}
		return result;
	}
}
