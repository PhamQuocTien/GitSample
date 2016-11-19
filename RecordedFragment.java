package com.pokara.singkaraokefree.ui.fragment;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.pokara.singkaraokefree.R;
import com.pokara.singkaraokefree.commons.CommonUtils;
import com.pokara.singkaraokefree.commons.Constants;
import com.pokara.singkaraokefree.model.Record;
import com.pokara.singkaraokefree.ui.activity.DetailRecordedActivity;

import java.io.File;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

/**
 * Created by maxo on 17/08/2016.
 */
public class RecordedFragment extends Fragment {
    @BindView(R.id.lvRecorded)
    RecyclerView lvRecorded;
    @BindView(R.id.tvRecordedNotification)
    TextView tvRecordedNotification;

    private RealmConfiguration realmConfig;
    private Realm realm;
    private RealmResults<Record> results;
    private ArrayList<Record> records;
    private ListRecordedAdapter listRecordedAdapter;
    private LinearLayoutManager layoutManager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recorded, container, false);
        ButterKnife.bind(this, view);
        initViews();
        return view;
    }

    private void initViews() {
        realmConfig = new RealmConfiguration.Builder(getContext()).deleteRealmIfMigrationNeeded().build();
        realm = Realm.getInstance(realmConfig);
        results = realm.where(Record.class).findAll();
        records = new ArrayList<Record>();
        for (int i = results.size() - 1; i >= 0; i--) {
            records.add(results.get(i));
        }
        if (records.isEmpty())
            tvRecordedNotification.setVisibility(View.VISIBLE);
        layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        lvRecorded.setLayoutManager(layoutManager);
        listRecordedAdapter = new ListRecordedAdapter(records);
        lvRecorded.setAdapter(listRecordedAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (CommonUtils.getSharedPreferences(getContext()).getBoolean(Constants.kReload, Boolean.FALSE)) {
            results = realm.where(Record.class).findAll();
            records.clear();
            for (int i = results.size() - 1; i >= 0; i--) {
                records.add(results.get(i));
            }
            listRecordedAdapter.notifyDataSetChanged();
            if (records.isEmpty())
                tvRecordedNotification.setVisibility(View.VISIBLE);
            CommonUtils.getSharedPreferences(getContext()).edit().putBoolean(Constants.kReload, Boolean.TRUE).commit();
        }
    }

    private void deleteRecord(Record record, int position) {
        try {
            File file = new File(record.filePath);
            file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        realm.beginTransaction();
        results.get(records.size() - position - 1).deleteFromRealm();
        realm.commitTransaction();
    }

    public class ListRecordedAdapter extends RecyclerView.Adapter<ListRecordedAdapter.MyViewHolder> {
        private ArrayList<Record> records;

        public ListRecordedAdapter(ArrayList<Record> records) {
            this.records = records;
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_listview_recorded, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final MyViewHolder holder, final int position) {
            final Record record = records.get(position);
            if (record.isVideo) {
                Glide.with(getContext()).load(record.filePath).into(holder.imgRecordedThumbnail);
            } else {
                holder.imgRecordedThumbnail.setImageResource(R.drawable.img_recorded_notcamera);
            }

            holder.tvRecordedTitle.setText(record.name);
            holder.tvRecordedDate.setText("Date: " + record.createAt);

            holder.rlRecorded.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getContext(), DetailRecordedActivity.class);
                    intent.putExtra(Constants.kPositionRecorded, position);
                    startActivity(intent);
                }
            });

            holder.imgRecordedMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final PopupMenu popup = new PopupMenu(getContext(), v);
                    popup.getMenuInflater().inflate(R.menu.menu_recorded, popup.getMenu());
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            int id = item.getItemId();
                            if (id == R.id.itemRecordedSave) {
                                File source = new File(record.filePath);
                                try {
                                    addVideo(source);
                                    Toast.makeText(getContext(), "File is saved to videos folder", Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(getContext(), "Have problem . Please try again later", Toast.LENGTH_SHORT).show();
                                }
                            } else if (id == R.id.itemRecordedDelete) {
                                deleteRecord(record, position);
                                records.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, records.size());
                            }
                            return true;
                        }
                    });
                    popup.show();
                }


            });

        }

        @Override
        public int getItemCount() {
            return records.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            View convertView;
            RelativeLayout rlRecorded;
            ImageView imgRecordedMenu;
            ImageView imgRecordedThumbnail;
            TextView tvRecordedTitle;
            TextView tvRecordedDate;

            public MyViewHolder(View convertView) {
                super(convertView);
                this.convertView = convertView;
                rlRecorded = (RelativeLayout) convertView.findViewById(R.id.rlRecorded);
                imgRecordedThumbnail = (ImageView) convertView.findViewById(R.id.imgRecordedThumbnail);
                imgRecordedMenu = (ImageView) convertView.findViewById(R.id.imgRecordedMenu);
                tvRecordedTitle = (TextView) convertView.findViewById(R.id.tvRecordedTitle);
                tvRecordedDate = (TextView) convertView.findViewById(R.id.tvRecordedDate);
            }
        }
    }

    public Uri addVideo(File videoFile) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.TITLE, videoFile.getName());
        values.put(MediaStore.Video.Media.DATA, videoFile.getAbsolutePath());
        return getContext().getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
    }
}
