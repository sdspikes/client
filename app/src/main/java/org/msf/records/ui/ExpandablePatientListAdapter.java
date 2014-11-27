package org.msf.records.ui;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorTreeAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.msf.records.R;
import org.msf.records.model.Status;
import org.msf.records.sync.PatientProviderContract;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by Gil on 24/11/14.
 */
public class ExpandablePatientListAdapter extends CursorTreeAdapter {

    private static final String TAG = ExpandablePatientListAdapter.class.getSimpleName();

    /**
     * Projection for querying the content provider.
     */
    private static final String[] PROJECTION = new String[] {
            PatientProviderContract.PatientColumns._ID,
            PatientProviderContract.PatientColumns.COLUMN_NAME_LOCATION_TENT,
            PatientProviderContract.PatientColumns.COLUMN_NAME_GIVEN_NAME,
            PatientProviderContract.PatientColumns.COLUMN_NAME_FAMILY_NAME,
            PatientProviderContract.PatientColumns.COLUMN_NAME_UUID,
            PatientProviderContract.PatientColumns.COLUMN_NAME_STATUS,
            PatientProviderContract.PatientColumns.COLUMN_NAME_ADMISSION_TIMESTAMP
    };

    // Constants representing column positions from PROJECTION.
    public static final int COLUMN_ID = 0;
    public static final int COLUMN_LOCATION_TENT = 1;
    public static final int COLUMN_GIVEN_NAME = 2;
    public static final int COLUMN_FAMILY_NAME = 3;
    public static final int COLUMN_UUID = 4;
    public static final int COLUMN_STATUS = 5;
    public static final int COLUMN_ADMISSION_TIMESTAMP = 6;

    private Context mContext;

    public ExpandablePatientListAdapter(Cursor cursor, Context context) {
        super(cursor, context);
        mContext = context;
    }

    @Override
    protected Cursor getChildrenCursor(Cursor groupCursor) {
        Cursor itemCursor = getGroup(groupCursor.getPosition());

        String tent = itemCursor.getString(PatientListFragment.COLUMN_LOCATION_TENT);
        Log.d(TAG, "Getting child cursor for tent: " + tent);

        CursorLoader cursorLoader = new CursorLoader(mContext,
                PatientProviderContract.CONTENT_URI,
                PROJECTION,
                PatientProviderContract.PatientColumns.COLUMN_NAME_LOCATION_TENT + "=?",
                new String[] { tent },
                null);

        Cursor childCursor = null;

        try {
            childCursor = cursorLoader.loadInBackground();
            Log.d(TAG, "childCursor " + childCursor.getCount());
            childCursor.moveToFirst();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return childCursor;
    }

    @Override
    protected View newGroupView(Context context, Cursor cursor, boolean isExpanded, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.listview_tent_header, null);
    }

    @Override
    protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
        int patientCount = getChildrenCursor(cursor).getCount();

        TextView item = (TextView) view.findViewById(R.id.patient_list_tent_tv);
        if (patientCount == 1) {
            item.setText(
                cursor.getString(PatientListFragment.COLUMN_LOCATION_TENT) + " " +
                context.getResources().getString(R.string.one_patient));
        } else {
            item.setText(
                cursor.getString(PatientListFragment.COLUMN_LOCATION_TENT) + " " +
                context.getResources().getString(R.string.n_patients, patientCount));
        }
    }

    @Override
    protected View newChildView(Context context, Cursor cursor, boolean isLastChild, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.listview_cell_search_results, parent, false);
        ViewHolder holder = new ViewHolder(view);
        view.setTag(holder);
        return view;
    }

    @Override
    protected void bindChildView(View convertView, Context context, Cursor cursor, boolean isLastChild) {

        ViewHolder holder = null;
        if (convertView != null) {
            holder = (ViewHolder) convertView.getTag();
        }

        String givenName = cursor.getString(COLUMN_GIVEN_NAME);
        String familyName = cursor.getString(COLUMN_FAMILY_NAME);
        String id = cursor.getString(COLUMN_ID);
        String status = cursor.getString(COLUMN_STATUS);

        holder.mPatientName.setText(givenName + " " + familyName);
        holder.mPatientId.setText(id);

        //Age currently not being stored in content provider
        /*if (patient.age.type != null && patient.age.type.equals("months")) {
            holder.mPatientAge.setText("<1");
        }

        if (patient.age.type != null && patient.age.type.equals("years")) {
            holder.mPatientAge.setText("" + patient.age.years);
        }

        if (patient.age.type == null) {
            holder.mPatientAge.setText("99");
            holder.mPatientAge.setTextColor(context.getResources().getColor(R.color.transparent));
        }*/

        //Gender currently not being stored in content provider
        /*if (patient.gender != null && patient.gender.equals("M")) {
            holder.mPatientGender.setImageDrawable(context.getResources().getDrawable(R.drawable.gender_man));
        }

        if (patient.gender != null && patient.gender.equals("F")) {
            if (patient.pregnant != null && patient.pregnant) {
                holder.mPatientGender.setImageDrawable(context.getResources().getDrawable(R.drawable.gender_pregnant));
            } else {
                holder.mPatientGender.setImageDrawable(context.getResources().getDrawable(R.drawable.gender_woman));
            }
        }

        if (patient.gender == null) {
            holder.mPatientGender.setVisibility(View.GONE);
        }*/

        if (status == null) {
            holder.mPatientId.setBackgroundColor(context.getResources().getColor(R.color.transparent));
            holder.mPatientId.setTextColor(context.getResources().getColor(android.R.color.black));
        }

        if (status != null && Status.getStatus(status) != null) {
            holder.mPatientId.setBackgroundColor(context.getResources().getColor(Status.getStatus(status).colorId));
            holder.mPatientId.setTextColor(context.getResources().getColor(R.color.white));
        }

        // Add a bottom border and extra padding to the last item in each group.
        if (isLastChild) {
            convertView.setBackgroundResource(R.drawable.bottom_border_1dp);
            convertView.setPadding(
                    convertView.getPaddingLeft(), convertView.getPaddingTop(),
                    convertView.getPaddingRight(), 40);
        } else {
            convertView.setBackgroundResource(0);
            convertView.setPadding(
                    convertView.getPaddingLeft(), convertView.getPaddingTop(),
                    convertView.getPaddingRight(), 20);
        }
    }

    static class ViewHolder {
        @InjectView(R.id.listview_cell_search_results_name) TextView mPatientName;
        @InjectView(R.id.listview_cell_search_results_id) TextView mPatientId;
        @InjectView(R.id.listview_cell_search_results_gender) ImageView mPatientGender;
        @InjectView(R.id.listview_cell_search_results_age) TextView mPatientAge;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}
