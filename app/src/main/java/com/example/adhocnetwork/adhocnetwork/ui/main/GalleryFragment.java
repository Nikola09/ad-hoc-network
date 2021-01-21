package com.example.adhocnetwork.adhocnetwork.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adhocnetwork.adhocnetwork.MyData;
import com.example.adhocnetwork.adhocnetwork.R;

import java.util.ArrayList;

public class GalleryFragment extends Fragment {
    ArrayList<CreateList> imgLists;
    private MyData mMyData;

    public GalleryFragment() {
        // Required empty public constructor
    }

    public static GalleryFragment newInstance() {
        GalleryFragment fragment = new GalleryFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mMyData = MyData.getInstance();
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView = (RecyclerView) getView().findViewById(R.id.imageGallery);
        recyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getActivity().getApplicationContext(),2);
        recyclerView.setLayoutManager(layoutManager);
        imgLists = mMyData.returnImages();
        GalleryAdapter adapter = new GalleryAdapter(getActivity().getApplicationContext(), imgLists);
        recyclerView.setAdapter(adapter);
    }

}