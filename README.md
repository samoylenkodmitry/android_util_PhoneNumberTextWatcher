# android_util_PhoneNumberTextWatcher
An implementation of phone number formatter that can be applied to Android's EditText

Use Case:

1. find your EditText: EditText mInputPhoneEdit;
2. create watcher: final PhoneNumberTextWatcher textWatcher = new PhoneNumberTextWatcher(mInputPhoneEdit, null);

3. choose format insertions: textWatcher.setPhoneFormatInsertions(PhoneNumberTextWatcher.PHONE_FORMAT_INSERTIONS_WITH_BRACES_AND_MINUSES);

4. apply watcher to editText: mInputPhoneEdit.addTextChangedListener(textWatcher);

5. optional, apply listener:

textWatcher.setPhoneNumberChangeListener(new PhoneNumberTextWatcher.OnPhoneNumberChangeListener() {
			@Override
			public void onPhoneNumberValid(final String phoneNumber) {
         //do something with valid number
			}

			@Override
			public void onPhoneNumberInvalid(final String digits) {
         //do something with invalid number
			}
		});

Where format insertions is something like this (place and character):
	public static final NumberInsertion[] PHONE_FORMAT_INSERTIONS_WITH_BRACES = {
		new NumberInsertion(0, OPEN_BRACE_STRING),
		new NumberInsertion(3, CLOSE_BRACE_STRING),
		new NumberInsertion(6, SPACE_STRING),
		new NumberInsertion(8, SPACE_STRING),
	};

