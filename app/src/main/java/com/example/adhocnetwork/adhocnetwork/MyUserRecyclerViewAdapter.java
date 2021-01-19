package com.example.adhocnetwork.adhocnetwork;

import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class MyUserRecyclerViewAdapter extends RecyclerView.Adapter<MyUserRecyclerViewAdapter.ViewHolder> {

    private final List<UserData> mValues;
    private ClickListener clickListener;

    public MyUserRecyclerViewAdapter(List<UserData> items) {
        mValues = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_users, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mNameView.setText(mValues.get(position).ShowName);
        String distanceStr;
        if (mValues.get(position).Distance < 0 || mValues.get(position).Distance >= 200)
            distanceStr = "Distance: Not Available";
        else
            distanceStr= "Distance: " + Math.round(mValues.get(position).Distance)+ "m";
        holder.mDistanceView.setText(distanceStr);
        holder.mSensorView.setText(MyData.getUserSensorsString(mValues.get(position)));
        holder.mHealthSensorView.setText(MyData.getUserFakeHealthSensorsString(mValues.get(position)));
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final View mView;
        public final TextView mNameView;
        public final TextView mDistanceView;
        public final TextView mSensorView;
        public final TextView mHealthSensorView;
        public UserData mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mNameView = (TextView) view.findViewById(R.id.item_number);
            mDistanceView = (TextView) view.findViewById(R.id.content);
            mSensorView = (TextView) view.findViewById(R.id.user_sensor);
            mHealthSensorView = (TextView) view.findViewById(R.id.user_health_sensor);

            if (clickListener != null) {
                itemView.setOnClickListener(this);
            }
        }

        @Override
        public String toString() {
            return super.toString() + mNameView.getText() + mDistanceView.getText()  + mSensorView.getText() + mHealthSensorView.getText();
        }

        @Override
        public void onClick(View v) {
            if (clickListener != null) {
                clickListener.onItemClick(getAdapterPosition(), v);
            }
        }
    }

    void setOnItemClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public interface ClickListener {
        void onItemClick(int position, View v);
    }

}