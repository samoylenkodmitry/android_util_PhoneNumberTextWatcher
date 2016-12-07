import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.EditText;

import junit.framework.Assert;

public class PhoneNumberTextWatcher implements TextWatcher, View.OnFocusChangeListener {

	public static final int FULL_NUMBER_DIGITS_COUNT = 10;
	public static final CharSequence EMPTY_TEXT = new SpannableString("");
	public static final NumberInsertion[] EMPTY_FORMAT_INSERTIONS = new NumberInsertion[0];
	public static final SpannableString FIRST_INSERTION_STRING_COUNTRY_CODE_RUS = new SpannableString("+7 ");
	private static final Spannable SPACE_STRING = new SpannableString(" ");
	/**
	 * Do not use digits in insertion strings.
	 */
	public static final NumberInsertion[] PHONE_FORMAT_INSERTIONS = {
		new NumberInsertion(3, SPACE_STRING),
		new NumberInsertion(6, SPACE_STRING),
		new NumberInsertion(8, SPACE_STRING),
	};
	private static final Spannable MINUS_STRING = new SpannableString("-");
	private static final Spannable CLOSE_BRACE_STRING = new SpannableString(") ");
	private static final Spannable OPEN_BRACE_STRING = new SpannableString(" (");
	public static final NumberInsertion[] PHONE_FORMAT_INSERTIONS_WITH_BRACES = {
		new NumberInsertion(0, OPEN_BRACE_STRING),
		new NumberInsertion(3, CLOSE_BRACE_STRING),
		new NumberInsertion(6, SPACE_STRING),
		new NumberInsertion(8, SPACE_STRING),
	};
	public static final NumberInsertion[] PHONE_FORMAT_INSERTIONS_WITH_BRACES_AND_MINUSES = {
		new NumberInsertion(0, OPEN_BRACE_STRING),
		new NumberInsertion(3, CLOSE_BRACE_STRING),
		new NumberInsertion(6, MINUS_STRING),
		new NumberInsertion(8, MINUS_STRING),
	};

	public interface OnPhoneNumberChangeListener {

		void onPhoneNumberValid(final String phoneNumber);

		void onPhoneNumberInvalid(final String digits);
	}

	public interface PhoneNumberValidator {

		boolean validate(final String digitsString);
	}

	private final PhoneNumberValidator mPhoneNumberValidator = new PhoneNumberLengthValidator(FULL_NUMBER_DIGITS_COUNT);
	private final SparseBooleanArray mValidSelectionPositions = new SparseBooleanArray();
	private final EditText mEditText;
	private Spannable mFirstInsertionString;
	private NumberInsertion[] mPhoneFormatInsertions = PHONE_FORMAT_INSERTIONS_WITH_BRACES;
	private OnPhoneNumberChangeListener mPhoneNumberChangeListener;
	private boolean mInTextChange;
	private int mLastSelectedPos;
	private String mLastText;
	private int mNewSelPosition;
	private int mMaxDigitsCount = FULL_NUMBER_DIGITS_COUNT;
	private String mPhoneNumber;
	private boolean mIsValidNumber;

	public PhoneNumberTextWatcher(final EditText editText) {
		this(editText, FIRST_INSERTION_STRING_COUNTRY_CODE_RUS);
	}

	public PhoneNumberTextWatcher(final EditText editText, final Spannable firstInsertionString) {
		mFirstInsertionString = firstInsertionString;

		assertInsertionsDontHaveDigits(PHONE_FORMAT_INSERTIONS);

		mEditText = editText;
		mEditText.setOnFocusChangeListener(this);

		if (!TextUtils.isEmpty(mFirstInsertionString)) {
			mEditText.setText(mFirstInsertionString);
			mValidSelectionPositions.append(mFirstInsertionString.length(), true);
		}
	}

	public static void assertInsertionsDontHaveDigits(final NumberInsertion[] phoneFormatInsertions) {
		for (final NumberInsertion insertion : phoneFormatInsertions) {
			Assert.assertNotNull(insertion);
			Assert.assertFalse(hasDigitCharacter(insertion.mText));
		}
	}
	
	public static boolean hasDigitCharacter(final CharSequence chars) {
		final int len = chars.length();
		for (int i = 0; i < len; i++) {
			if (Character.isDigit(chars.charAt(i))) {

				return true;
			}
		}

		return false;
	}

	public static int getLength(final CharSequence chars) {
		return chars == null ? 0 : chars.length();
	}
	
	public static String getDigits(final String str) {
		if (TextUtils.isEmpty(str)) {

			return str;
		}
		final int sz = str.length();
		final char[] chs = new char[sz];
		int count = 0;
		for (int i = 0; i < sz; i++) {
			if (Character.isDigit(str.charAt(i))) {
				chs[count++] = str.charAt(i);
			}
		}
		if (count == sz) {

			return str;
		}

		return new String(chs, 0, count);
	}

	public static boolean startsWith(final CharSequence chars, final CharSequence prefix) {
		if (chars == null || prefix == null) {

			return false;
		}

		final int lenPrefix = prefix.length();

		if (lenPrefix > chars.length()) {

			return false;
		}

		for (int i = 0; i < lenPrefix; i++) {
			if (prefix.charAt(i) != chars.charAt(i)) {

				return false;
			}
		}

		return true;
	}

	public String getPhoneNumber() {
		return mPhoneNumber;
	}

	private void setPhoneNumber(final CharSequence phoneNumber) {
		final Editable text = mEditText.getText();
		text.replace(0, text.length(), phoneNumber, 0, phoneNumber.length());
	}

	public boolean isValidNumber() {
		return mIsValidNumber;
	}

	public void setMaxDigitsCount(final int maxDigitsCount) {
		mMaxDigitsCount = maxDigitsCount;
	}

	public void setPhoneFormatInsertions(final NumberInsertion[] phoneFormatInsertions) {
		assertInsertionsDontHaveDigits(phoneFormatInsertions);

		mPhoneFormatInsertions = phoneFormatInsertions;
	}

	public void setFirstInsertionString(final Spannable firstInsertionString) {
		mFirstInsertionString = firstInsertionString;

		applyFirstInsertionString();
	}

	public void setPhoneNumberChangeListener(final OnPhoneNumberChangeListener onPhoneNumberChangeListener) {
		mPhoneNumberChangeListener = onPhoneNumberChangeListener;

		validateCurrentNumber();
	}

	public boolean validateCurrentNumber() {
		final String text = mEditText.getText().toString();
		final String digitsString = getDigits(text.substring(
			Math.min(getLength(mFirstInsertionString), text.length())));

		return validateNumber(digitsString);
	}

	public void clear() {
		mInTextChange = true;
		setPhoneNumber(EMPTY_TEXT);
		mInTextChange = false;
		applyFirstInsertionString();
	}

	private void applyFirstInsertionString() {
		if (!TextUtils.isEmpty(mFirstInsertionString)) {
			final Editable text = mEditText.getText();

			if (!startsWith(text, mFirstInsertionString)) {
				mInTextChange = true;

				setPhoneNumber(mFirstInsertionString);

				mValidSelectionPositions.clear();
				mValidSelectionPositions.append(mFirstInsertionString.length(), true);

				mEditText.setSelection(mEditText.length());

				mInTextChange = false;
			}
		}
	}

	@Override
	public void onFocusChange(final View v, final boolean hasFocus) {
		if (hasFocus) {
			mEditText.setSelection(mEditText.length());
		}
	}

	@Override
	public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
		if (!mInTextChange) {
			if (s != null) {
				mLastText = s.toString();
			}
			mLastSelectedPos = mEditText.getSelectionStart();
		}
	}

	@Override
	public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
	}

	@Override
	public void afterTextChanged(final Editable s) {
		if (!mInTextChange) {
			mInTextChange = true;

			mNewSelPosition = mEditText.getSelectionStart();

			if (s != null && !TextUtils.equals(mLastText, s)) {

				final String digitsString = getDigits(s.toString().substring(
					Math.min(getLength(mFirstInsertionString), s.length())));

				if (digitsString.length() > mMaxDigitsCount) {
					setPhoneNumber(mLastText);
					mEditText.setSelection(mLastText.length());
				} else if (TextUtils.isEmpty(digitsString)) {
					clear();
				} else {
					formatAndApplyPhoneNumber(digitsString);
				}
			}

			applyFirstInsertionString();

			validateCurrentNumber();

			mInTextChange = false;
		}
	}

	private void formatAndApplyPhoneNumber(final CharSequence digitsString) {
		final SpannableStringBuilder formattedNumber = new SpannableStringBuilder();

		if (mFirstInsertionString != null) {
			formattedNumber.append(mFirstInsertionString);
		}
		mValidSelectionPositions.clear();
		mValidSelectionPositions.append(formattedNumber.length(), true);

		for (int charPos = 0; charPos < digitsString.length(); charPos++) {
			for (final NumberInsertion insertion : mPhoneFormatInsertions) {
				if (charPos == insertion.getPosition()) {
					formattedNumber.append(insertion.getText());
					mValidSelectionPositions.append(formattedNumber.length(), true);
				}
			}

			formattedNumber.append(digitsString.charAt(charPos));
			mValidSelectionPositions.append(formattedNumber.length(), true);
		}

		final int formattedLength = formattedNumber.length();
		final int charsAdded = formattedLength - (mLastText != null ? mLastText.length() : 0);

		setPhoneNumber(formattedNumber);

		int selection = mLastSelectedPos + charsAdded;
		if (selection < 0) {
			selection = 0;
		} else if (selection > formattedLength) {
			selection = formattedLength;
		}

		mEditText.setSelection(selection);
	}

	protected boolean validateNumber(final String digitsString) {
		mIsValidNumber = mPhoneNumberValidator.validate(digitsString);
		mPhoneNumber = digitsString;
		if (mPhoneNumberChangeListener != null) {
			if (mIsValidNumber) {
				mPhoneNumberChangeListener.onPhoneNumberValid(digitsString);
			} else {
				mPhoneNumberChangeListener.onPhoneNumberInvalid(digitsString);
			}
		}
		return mIsValidNumber;
	}

	public SparseBooleanArray getValidSelectionPositions() {

		return mValidSelectionPositions;
	}

	public int getNewSelPosition() {
		return mNewSelPosition;
	}

	public String getFormattedPhoneNumber() {
		return mEditText.getText().toString();
	}

	public static class NumberInsertion {

		private final int mPosition;
		private final Spannable mText;

		public NumberInsertion(final int position, final Spannable text) {
			mPosition = position;
			mText = text;
		}

		public int getPosition() {
			return mPosition;
		}

		public CharSequence getText() {
			return mText;
		}
	}

	public static class PhoneNumberLengthValidator implements PhoneNumberValidator {

		private final int mValidCount;

		public PhoneNumberLengthValidator(final int digitsCount) {
			mValidCount = digitsCount;
		}

		@Override
		public boolean validate(final String digitsString) {
			return !TextUtils.isEmpty(digitsString)
				&& digitsString.length() == mValidCount
				&& TextUtils.isDigitsOnly(digitsString);
		}
	}

	private static void applyColorToSpan(final int color, final Spannable openBraceString) {
		openBraceString.setSpan(new ForegroundColorSpan(color), 0, openBraceString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
	}
}
