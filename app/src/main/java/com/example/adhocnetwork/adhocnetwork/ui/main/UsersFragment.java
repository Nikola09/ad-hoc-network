package com.example.adhocnetwork.adhocnetwork.ui.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.adhocnetwork.adhocnetwork.ConnectionService;
import com.example.adhocnetwork.adhocnetwork.MyData;
import com.example.adhocnetwork.adhocnetwork.R;
import com.example.adhocnetwork.adhocnetwork.SendFilesActivity;
import com.example.adhocnetwork.adhocnetwork.SettingsActivity;
import com.example.adhocnetwork.adhocnetwork.UserData;

/**
 * A fragment representing a list of Items.
 */
public class UsersFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private MyData mMyData;
    UserRecyclerViewAdapter adapter;
    private TextView txtSensors;
    private TextView txtHealthSensors;

    private ConnectionService connectionService;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public UsersFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static UsersFragment newInstance(int columnCount) {
        UsersFragment fragment = new UsersFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_users_list, container, false);
        RecyclerView recyclerView=(RecyclerView)view.findViewById(R.id.list);

        mMyData = MyData.getInstance();
        connectionService = ConnectionService.getInstance();

        if (recyclerView != null) {
            Context context = view.getContext();
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            adapter = new UserRecyclerViewAdapter(mMyData.getUsers());

            /*adapter.setOnItemClickListener(new MyUserRecyclerViewAdapter.ClickListener() {
                @Override
                public void onItemClick(int position, View v) {
                    Toast.makeText(v.getContext(), "Click on " + position, Toast.LENGTH_SHORT).show();
                    // something on click
                }
            });*/
            recyclerView.setAdapter(adapter);
        }

        ImageButton btnSettings = view.findViewById(R.id.btnSettings);
        if (connectionService.Role == "Host")
        {
            btnSettings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(view.getContext(), SettingsActivity.class);
                    startActivity(intent);
                }

            });
        }
        else
        {
            btnSettings.setVisibility(View.INVISIBLE);
        }

        Button button = view.findViewById(R.id.btnSendFiles);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), SendFilesActivity.class);
                startActivity(intent);
            }

        });
        TextView txtName = view.findViewById(R.id.myName);
        txtName.setText(mMyData.getShowName());
        txtSensors = view.findViewById(R.id.sensors);
        txtHealthSensors = view.findViewById(R.id.healthSensors);
        updateMySensors();

        return view;
    }
    public void updateMySensors()
    {
        if (txtSensors != null)// OnCreate and OnCreateView are late
            txtSensors.setText(mMyData.getMySensorsString());
        if (txtHealthSensors != null)
            txtHealthSensors.setText(mMyData.getMyFakeHealthSensorsString());

    }

    public void insertSingleItem(UserData ud) {
        adapter.notifyItemInserted(mMyData.getUsers().indexOf(ud));
    }
    public void removeSingleItem(int index) {
        adapter.notifyItemRemoved(index);
    }
    public void updateSingleItem(int index) {
        if (adapter != null) {
            adapter.notifyItemChanged(index);
        }
    }

}