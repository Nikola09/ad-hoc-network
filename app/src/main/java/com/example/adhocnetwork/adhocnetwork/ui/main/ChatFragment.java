package com.example.adhocnetwork.adhocnetwork.ui.main;

import android.app.Activity;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.adhocnetwork.adhocnetwork.ChatMessage;
import com.example.adhocnetwork.adhocnetwork.ConnectionService;
import com.example.adhocnetwork.adhocnetwork.MyData;
import com.example.adhocnetwork.adhocnetwork.R;
import com.example.adhocnetwork.adhocnetwork.SendFilesActivity;
import com.example.adhocnetwork.adhocnetwork.UserData;
import com.google.android.gms.nearby.connection.Payload;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ChatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatFragment extends Fragment {

    private MyData mMyData;
    private ChatRecyclerViewAdapter adapter;
    private ConnectionService mConnectionService;

    private EditText etMsg;

    public ChatFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * <p>
     *
     * @return A new instance of fragment ChatFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ChatFragment newInstance() {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerChat);

        mMyData = MyData.getInstance();
        mConnectionService = ConnectionService.getInstance();

        if (recyclerView != null) {
            Context context = view.getContext();
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            adapter = new ChatRecyclerViewAdapter(mMyData.getMessages());
            recyclerView.setAdapter(adapter);

            recyclerView.scrollToPosition(mMyData.getMessages().size() - 1);

            etMsg = (EditText) view.findViewById(R.id.editChatMessage);
            Button button = view.findViewById(R.id.btnChatSend);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //get msg from field and send msg
                    String msg = etMsg.getText().toString();
                    //check is valid
                    if (msg.isEmpty() || msg.length() > 100) {
                        Toast.makeText(getContext(), "Please enter a valid message", Toast.LENGTH_LONG).show();
                    } else {
                        Date time = new java.util.Date(System.currentTimeMillis());
                        String timeString = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(time);
                        ChatMessage chatMessage = new ChatMessage(mMyData.getShowName(), timeString, msg, true);
                        try {
                            mMyData.addMessage(chatMessage);
                            insertSingleItem(chatMessage);
                            hideKeyboard();

                            Payload payload = Payload.fromBytes(MyData.serialize(chatMessage.sendThisMessage()));
                            mConnectionService.send(payload);

                            etMsg.setText("");
                        } catch (Exception e) {
                            System.out.println("ERROR - unexpected exception: " + e.getMessage());
                        }
                    }

                }

            });

        }
        return view;
    }

    public void insertSingleItem(ChatMessage cm) {
        adapter.notifyItemInserted(mMyData.getMessages().indexOf(cm));
    }

    public void hideKeyboard() {
        Activity activity = getActivity();
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}