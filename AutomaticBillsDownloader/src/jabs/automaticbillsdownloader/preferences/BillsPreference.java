package jabs.automaticbillsdownloader.preferences;

import jabs.automaticbillsdownloader.DropboxManager;
import jabs.automaticbillsdownloader.R;
import jabs.automaticbillsdownloader.model.Bill;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.preference.DialogPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class BillsPreference extends DialogPreference {
	private Spinner billTypeSpinner, dropboxFolderSpinner;
	private EditText usernameField, passwordField;
	private Bill lastBill;

	@SuppressLint("SimpleDateFormat")
	public BillsPreference(Context context) {
		this(context, (AttributeSet) null);
		
		String key = "billsPreference"
				+ new SimpleDateFormat("yyyyMMddHHmmssSSS").format(Calendar
						.getInstance().getTime());
		setKey(key);
		setIcon(R.drawable.document_add_icon);
		setTitle("Add New Bill");
	}
	
	public BillsPreference(Context context, String key) {
		this(context, (AttributeSet) null);
		setKey(key);
		setIcon(R.drawable.document_icon);
	}
	
	public BillsPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setPositiveButtonText("Set");
		setNegativeButtonText("Cancel");
	}

	@SuppressLint("InflateParams")
	@Override
	protected View onCreateDialogView() {
		LayoutInflater inflater = (LayoutInflater) getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View billPrefView = inflater.inflate(R.layout.preference_bill, null);
		
		billTypeSpinner = (Spinner) billPrefView.findViewById(R.id.bill_type_spinner);
		usernameField = (EditText) billPrefView.findViewById(R.id.username_edit_text_field);
		passwordField = (EditText) billPrefView.findViewById(R.id.password_edit_text_field);
		dropboxFolderSpinner = (Spinner) billPrefView.findViewById(R.id.dropbox_folder_spinner);
		Button deleteButton = (Button) billPrefView.findViewById(R.id.delete_bill_button);
		deleteButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				BillsPreference.this.getDialog().cancel();
				Editor editor = getPreferenceManager().getSharedPreferences().edit();
				editor.remove(getKey());
				editor.commit();
			}
		});
		
		TextWatcher textWatcher = new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			
			@Override
			public void afterTextChanged(Editable s) {
				enableDisableDialog();
			}
		};
		
		usernameField.addTextChangedListener(textWatcher);
		passwordField.addTextChangedListener(textWatcher);
		
		ArrayAdapter<CharSequence> billTypeAdapter = ArrayAdapter
				.createFromResource(getContext(), R.array.bill_types_array,
						android.R.layout.simple_spinner_item);
		billTypeAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		billTypeSpinner.setAdapter(billTypeAdapter);
		
		return billPrefView;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onBindDialogView(View v) {
		super.onBindDialogView(v);
		
		ArrayAdapter<CharSequence> billTypeAdapter = (ArrayAdapter<CharSequence>) billTypeSpinner
				.getAdapter();
		ArrayAdapter<CharSequence> dropboxFolderAdapter = (ArrayAdapter<CharSequence>) dropboxFolderSpinner
				.getAdapter();
		
		if(lastBill != null) {
			billTypeSpinner.setSelection(billTypeAdapter.getPosition(lastBill
					.getBillType()));
			usernameField.setText(lastBill.getUsername());
			passwordField.setText(lastBill.getPassword());
			if(dropboxFolderAdapter != null) {
				dropboxFolderSpinner.setSelection(dropboxFolderAdapter
						.getPosition(lastBill.getDropboxFolder()));
			}
		}
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult) {
			lastBill = new Bill(
					billTypeSpinner.getSelectedItem().toString(),
					usernameField.getText().toString(),
					passwordField.getText().toString(),
					dropboxFolderSpinner.getSelectedItem().toString());

			if (callChangeListener(lastBill.toJson())) {
				persistString(lastBill.toJson());
				this.setTitle(lastBill.toString());
				this.setSummary(lastBill.getDropboxFolder());
			}
		}
	}
	
	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return (a.getString(index));
	}
	
	
	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		Bill bill = null;

		if (restoreValue) {
			if (defaultValue == null) {
				bill = Bill.createFromJson(getPersistedString(""));
			} else {
				bill = Bill.createFromJson(getPersistedString(defaultValue.toString()));
			}
		} else {
			bill = Bill.createFromJson(defaultValue.toString());
		}

		lastBill = bill;
		
		if(bill != null) {
			this.setTitle(bill.toString());
			this.setSummary(bill.getDropboxFolder());
		}
	}
	
	@Override
	protected void onClick() {
		super.onClick();
		
		enableDisableDialog();
		new AsyncTask<Void, Void, ArrayAdapter<CharSequence>>() {
			@Override
			protected ArrayAdapter<CharSequence> doInBackground(Void... params) {
				List<CharSequence> dropboxFolderList = new ArrayList<CharSequence>();
				try {
					Entry entries = DropboxManager.instance().metadata("", 100, null, true, null);
					for (Entry e : entries.contents) {
					    if (e.isDir && !e.isDeleted) {
					    	dropboxFolderList.add(e.path);
					    }
					}
				} catch (DropboxException e) {
					e.printStackTrace();
				}
				
				ArrayAdapter<CharSequence> dropboxFolderAdapter = new ArrayAdapter<CharSequence>(
						getContext(), android.R.layout.simple_spinner_item,
						dropboxFolderList);
				dropboxFolderAdapter
						.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				return dropboxFolderAdapter;
			}
			
			@Override
			protected void onPostExecute(ArrayAdapter<CharSequence> result) {
				dropboxFolderSpinner.setAdapter(result);
				if(lastBill != null) {
					dropboxFolderSpinner.setSelection(result
							.getPosition(lastBill.getDropboxFolder()));
				}
				enableDisableDialog();
			}
		}.execute();
	}

	private void enableDisableDialog() {
		final AlertDialog dialog = (AlertDialog) getDialog();
		
		if(dialog == null)
			return;
		
		dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
		
		if (billTypeSpinner.getSelectedItemId() != Spinner.INVALID_ROW_ID
				&& usernameField.getText() != null
				&& !usernameField.getText().toString().trim().isEmpty()
				&& passwordField.getText() != null
				&& !passwordField.getText().toString().isEmpty()
				&& dropboxFolderSpinner.getSelectedItemId() != Spinner.INVALID_ROW_ID) {
			dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
		}
	}
}
