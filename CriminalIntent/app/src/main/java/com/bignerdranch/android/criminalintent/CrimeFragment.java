package com.bignerdranch.android.criminalintent;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.SparseArrayCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import org.w3c.dom.Text;

import java.io.File;
import java.util.Date;
import java.util.UUID;

public class CrimeFragment extends Fragment {

    private static final String ARG_CRIME_ID = "crime_id";
    private static final String DIALOG_DATE = "DialogDate";

    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_CONTACT = 1;
    private static final int REQUEST_PHOTO= 2;

    private Crime mCrime;
    private File mPhotoFile;
    private File mPhotoFile2;
    private File mPhotoFile3;
    private File mPhotoFile4;
    private EditText mTitleField;
    private Button mDateButton;
    private CheckBox mSolvedCheckbox;
    private Button mReportButton;
    private Button mSuspectButton;
    private ImageButton mPhotoButton;
    private ImageView mPhotoView;
    private ImageView mPhotoView2;
    private ImageView mPhotoView3;
    private ImageView mPhotoView4;
    private CheckBox mFaceDetect;
    private boolean mCanTakePhoto;
    private TextView mFaceCount;
    private Paint mPaint;
    private SparseArray<Face> mFaces;

    public static CrimeFragment newInstance(UUID crimeId) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeId);

        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UUID crimeId = (UUID) getArguments().getSerializable(ARG_CRIME_ID);
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
        mPhotoFile = CrimeLab.get(getActivity()).getPhotoFile(mCrime, mCrime.getPhotoCount());
        mPhotoFile2 = CrimeLab.get(getActivity()).getPhotoFile(mCrime, mCrime.getPhotoCount()-1);
        mPhotoFile3 = CrimeLab.get(getActivity()).getPhotoFile(mCrime, mCrime.getPhotoCount()-2);
        mPhotoFile4 = CrimeLab.get(getActivity()).getPhotoFile(mCrime, mCrime.getPhotoCount()-3);
        mPaint = new Paint();
    }

    @Override
    public void onPause() {
        super.onPause();

        CrimeLab.get(getActivity())
                .updateCrime(mCrime);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_crime, container, false);

        mTitleField = (EditText) v.findViewById(R.id.crime_title);
        mTitleField.setText(mCrime.getTitle());
        mTitleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCrime.setTitle(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mDateButton = (Button) v.findViewById(R.id.crime_date);
        updateDate();
        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager manager = getFragmentManager();
                DatePickerFragment dialog = DatePickerFragment
                        .newInstance(mCrime.getDate());
                dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
                dialog.show(manager, DIALOG_DATE);
            }
        });

        mSolvedCheckbox = (CheckBox) v.findViewById(R.id.crime_solved);
        mSolvedCheckbox.setChecked(mCrime.isSolved());
        mSolvedCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCrime.setSolved(isChecked);
            }
        });

        mFaceDetect = (CheckBox) v.findViewById((R.id.enableFace_checkBox));
        mFaceDetect.setChecked(mCrime.isFaceDetect());
        mFaceDetect.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCrime.setFaceDetect(isChecked);
                updatePhotoView();
            }
        });

        mReportButton = (Button)v.findViewById(R.id.crime_report);
        mReportButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, getCrimeReport());
                i.putExtra(Intent.EXTRA_SUBJECT,
                        getString(R.string.crime_report_subject));
                i = Intent.createChooser(i, getString(R.string.send_report));

                startActivity(i);
            }
        });

        final Intent pickContact = new Intent(Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI);
        mSuspectButton = (Button)v.findViewById(R.id.crime_suspect);
        mSuspectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(pickContact, REQUEST_CONTACT);
            }
        });

        if (mCrime.getSuspect() != null) {
            mSuspectButton.setText(mCrime.getSuspect());
        }

        PackageManager packageManager = getActivity().getPackageManager();
        if (packageManager.resolveActivity(pickContact,
                PackageManager.MATCH_DEFAULT_ONLY) == null) {
            mSuspectButton.setEnabled(false);
        }

        mFaceCount = (TextView) v.findViewById(R.id.faceCount);

        mPhotoButton = (ImageButton) v.findViewById(R.id.crime_camera);
        final Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        mCanTakePhoto = mPhotoFile != null &&
                captureImage.resolveActivity(packageManager) != null;
        mPhotoButton.setEnabled(mCanTakePhoto);

        //if the count > 1, call this function

        mPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCrime.incrementPhotoCount();
                mPhotoFile = CrimeLab.get(getActivity()).getPhotoFile(mCrime, mCrime.getPhotoCount());
                mPhotoFile2 = CrimeLab.get(getActivity()).getPhotoFile(mCrime, mCrime.getPhotoCount()-1);
                mPhotoFile3 = CrimeLab.get(getActivity()).getPhotoFile(mCrime, mCrime.getPhotoCount()-2);
                mPhotoFile4 = CrimeLab.get(getActivity()).getPhotoFile(mCrime, mCrime.getPhotoCount()-3);
                if (mCanTakePhoto) {
                    Uri uri = Uri.fromFile(mPhotoFile);
                    captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                }
                startActivityForResult(captureImage, REQUEST_PHOTO);
            }
        });

        mPhotoView = (ImageView) v.findViewById(R.id.crime_photo);
        mPhotoView2 = (ImageView) v.findViewById(R.id.crime_image2);
        mPhotoView3 = (ImageView) v.findViewById(R.id.crime_image3);
        mPhotoView4 = (ImageView) v.findViewById(R.id.crime_image4);
        updatePhotoView();

        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            if (requestCode == REQUEST_PHOTO) {
                mCrime.setPhotoCount(mCrime.getPhotoCount() - 1);
            }
            return;
        }

        if (requestCode == REQUEST_DATE) {
            Date date = (Date) data
                    .getSerializableExtra(DatePickerFragment.EXTRA_DATE);
            mCrime.setDate(date);
            updateDate();
        } else if (requestCode == REQUEST_CONTACT && data != null) {
            Uri contactUri = data.getData();
            // Specify which fields you want your query to return
            // values for.
            String[] queryFields = new String[] {
                    ContactsContract.Contacts.DISPLAY_NAME,
            };
            // Perform your query - the contactUri is like a "where"
            // clause here
            ContentResolver resolver = getActivity().getContentResolver();
            Cursor c = resolver
                    .query(contactUri, queryFields, null, null, null);

            try {
                // Double-check that you actually got results
                if (c.getCount() == 0) {
                    return;
                }

                // Pull out the first column of the first row of data -
                // that is your suspect's name.
                c.moveToFirst();

                String suspect = c.getString(0);
                mCrime.setSuspect(suspect);
                mSuspectButton.setText(suspect);
            } finally {
                c.close();
            }
        } else if (requestCode == REQUEST_PHOTO) {
            updatePhotoView();
        }
    }

    private void updateDate() {
        mDateButton.setText(mCrime.getDate().toString());
    }

    private String getCrimeReport() {
        String solvedString = null;
        if (mCrime.isSolved()) {
            solvedString = getString(R.string.crime_report_solved);
        } else {
            solvedString = getString(R.string.crime_report_unsolved);
        }
        String dateFormat = "EEE, MMM dd";
        String dateString = DateFormat.format(dateFormat, mCrime.getDate()).toString();
        String suspect = mCrime.getSuspect();
        if (suspect == null) {
            suspect = getString(R.string.crime_report_no_suspect);
        } else {
            suspect = getString(R.string.crime_report_suspect, suspect);
        }
        String report = getString(R.string.crime_report,
                mCrime.getTitle(), dateString, solvedString, suspect);
        return report;
    }

    private void updatePhotoView() {
        if (mPhotoFile == null || !mPhotoFile.exists()) {
            mPhotoView.setImageDrawable(null);
        } else {
            Bitmap bitmap = PictureUtils.getScaledBitmap(
                    mPhotoFile.getPath(), getActivity());

            Matrix matrix = new Matrix();
            matrix.postRotate(-90);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap , 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            if(mFaceDetect.isChecked()){
                FaceDetector detector = new FaceDetector.Builder( getContext() )
                        .setTrackingEnabled(false)
                        .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                        .setMode(FaceDetector.FAST_MODE)
                        .build();
                if (!detector.isOperational()) {
                    //Handle contingency
                } else {
                    Frame frame = new Frame.Builder().setBitmap(rotatedBitmap).build();
                    mFaces = detector.detect(frame);
                    detector.release();
                    setNumFaceText(mFaces.size());
                }
                //invalidate(); //NOT SURE???

                Bitmap tempBitmap = Bitmap.createBitmap(rotatedBitmap.getWidth(), rotatedBitmap.getHeight(), Bitmap.Config.RGB_565);
                Canvas tempCanvas = new Canvas(tempBitmap);

                tempCanvas.drawBitmap(rotatedBitmap, 0, 0, null);
                drawFaceBox(tempCanvas, 1);


                mPhotoView.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
            }
            else {
                mPhotoView.setImageBitmap(rotatedBitmap);
                setNumFaceText(-1);
            }
        }

        if (mPhotoFile2 == null || !mPhotoFile2.exists()) {
            mPhotoView2.setImageResource(R.drawable.placeholder);
        } else {
            Bitmap bitmap = PictureUtils.getScaledBitmap(
                    mPhotoFile2.getPath(), getActivity());
            mPhotoView2.setImageBitmap(bitmap);
        }

        if (mPhotoFile3 == null || !mPhotoFile3.exists()) {
            mPhotoView3.setImageResource(R.drawable.placeholder);
        } else {
            Bitmap bitmap = PictureUtils.getScaledBitmap(
                    mPhotoFile3.getPath(), getActivity());
            mPhotoView3.setImageBitmap(bitmap);
        }

        if (mPhotoFile4 == null || !mPhotoFile4.exists()) {
            mPhotoView4.setImageResource(R.drawable.placeholder);
        } else {
            Bitmap bitmap = PictureUtils.getScaledBitmap(
                    mPhotoFile4.getPath(), getActivity());
            mPhotoView4.setImageBitmap(bitmap);
        }
    }

    private void drawFaceBox(Canvas canvas, double scale) {
        //paint should be defined as a member variable rather than
        //being created on each onDraw request, but left here for
        //emphasis.

        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(5);

        float left = 0;
        float top = 0;
        float right = 0;
        float bottom = 0;

        for( int i = 0; i < mFaces.size(); i++ ) {
            Face face = mFaces.valueAt(i);

            left = (float) ( face.getPosition().x * scale );
            top = (float) ( face.getPosition().y * scale );
            right = (float) scale * ( face.getPosition().x + face.getWidth() );
            bottom = (float) scale * ( face.getPosition().y + face.getHeight() );

            canvas.drawRect( left, top, right, bottom, mPaint );
        }
    }
    private void  setNumFaceText(int faceCount){
        if (faceCount == 0) {
            mFaceCount.setText("No Faces Detected");
        } else if (faceCount == 1) {
            mFaceCount.setText("1 face found");
        } else if (faceCount == -1) {
            mFaceCount.setText("Face detection off");
        } else {
            mFaceCount.setText(faceCount + " faces found");
        }
    }
}
