package com.ssafy.recto.mypage;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.ssafy.recto.MainActivity;
import com.ssafy.recto.R;
import com.ssafy.recto.api.ApiInterface;
import com.ssafy.recto.api.CardData;
import com.ssafy.recto.api.HttpClient;
import com.ssafy.recto.config.MediaScanner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import lombok.SneakyThrows;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragmentMineDetail2 extends Fragment {

    private static final boolean isLegacy = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q;
    ApiInterface api;
    MainActivity mainActivity;
    ImageView cardImageView;
    ImageView info_dialog;
    ImageView lock;
    ImageView mine_photo_card_list_btn;
    TextView tv_phrases;
    TextView card_id;
    TextView card_date;
    Button download_button;
    ImageView delete_button;
    Button cart_button;
    ImageView cart_count;
    private View view;
    private Context mContext;
    int seq;
    String photo_url;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mainActivity = (MainActivity)getActivity();
        mContext = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mainActivity = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.profile_fragment_mine_card_detail2, container, false);
        api = HttpClient.getRetrofit().create( ApiInterface.class );

        // 카드 목록에서 photo_seq 값 (sep[pos]) 가져오기
        Bundle bundle = getArguments();
        if (bundle != null) {
            seq = bundle.getInt("seq");
        }

        try {
            requestGet();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        card_date = view.findViewById(R.id.card_date);
        tv_phrases = view.findViewById(R.id.tv_phrases);
        card_id = view.findViewById(R.id.card_id);
        cardImageView = view.findViewById(R.id.card_image_detail);
        mine_photo_card_list_btn = view.findViewById(R.id.mine_photo_card_list_btn);
        info_dialog = view.findViewById(R.id.info_dialog);
        download_button = view.findViewById(R.id.download_button);
        delete_button = view.findViewById(R.id.delete_button);
        cart_button = view.findViewById(R.id.cart_button);
        lock = view.findViewById(R.id.lock);
        cart_count = mainActivity.findViewById(R.id.cart_count);

        // 목록보기 버튼
        mine_photo_card_list_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.setFragment("profile");
            }
        });

        // 버튼 눌렀을 때 카드 갤러리에 저장하기
        download_button.setOnClickListener(new View.OnClickListener() {
            @SneakyThrows
            @Override
            public void onClick(View v) {

                final FrameLayout capture = view.findViewById(R.id.card_frameLayout);

                SimpleDateFormat day = new SimpleDateFormat("yyyyMMddmmss");
                Date date = new Date();
                capture.buildDrawingCache();
                Bitmap captureview = capture.getDrawingCache();

                FileOutputStream fos;
                if(isLegacy) {
                    try {
                        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/RECTO";
                        File file = new File(path);
                        if (!file.exists()){
                            file.mkdir();
                        }

                        fos = new FileOutputStream(path + "/RECTO" + day.format(date) + ".JPEG");
                        captureview.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        MediaScanner.newInstance(getContext()).mediaScanning(path + "/RECTO" + day.format(date) + ".JPEG");
                        Toast.makeText(getContext(), "저장이 완료되었습니다", Toast.LENGTH_SHORT).show();
                        fos.flush();
                        fos.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    String path = Environment.DIRECTORY_DCIM + "/RECTO";
                    File file = new File(path);
                    if (!file.exists()){
                        file.mkdir();
                    }

                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.RELATIVE_PATH, path);
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, "RECTO" + day.format(date));
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/JPEG");

                    ContentResolver contentResolver = getContext().getContentResolver();
                    Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                    Uri content = contentResolver.insert(collection, values);
                    try{
                        ParcelFileDescriptor pdf = contentResolver.openFileDescriptor(content, "w", null);

                        fos = new FileOutputStream(pdf.getFileDescriptor());
                        captureview.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        Toast.makeText(getContext(), "저장이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                        fos.flush();
                        fos.close();
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                }
                capture.destroyDrawingCache();

            }
        });

        // 삭제 버튼 눌렀을 때 다이얼로그
        delete_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder ad = new AlertDialog.Builder(getContext());
                ad.setTitle("Delete card");
                ad.setMessage("카드를 삭제하시겠습니까?");

                // 삭제 버튼
                ad.setPositiveButton("삭제", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 카드 삭제 api
                        try {
                            Call<String> call = api.deleteCard(seq);
                            call.enqueue(new Callback<String>() {
                                @Override
                                public void onResponse(Call<String> call, Response<String> response) {
                                    Toast.makeText(getContext(), "삭제가 완료되었습니다.", Toast.LENGTH_SHORT).show();
                                    mainActivity.setFragment("profile");
                                }

                                @Override
                                public void onFailure(Call<String> call, Throwable t) {
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        // 다이얼로그 닫기
                        dialog.dismiss();
                    }
                });

                // 취소 버튼
                ad.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 다이얼로그 닫기
                        dialog.dismiss();
                    }
                });

                AlertDialog alertAd = ad.create();
                alertAd.show();

                Button yes_btn = alertAd.getButton(DialogInterface.BUTTON_POSITIVE);
                yes_btn.setTextColor(Color.RED);

                Button no_btn = alertAd.getButton(DialogInterface.BUTTON_NEGATIVE);
                no_btn.setTextColor(Color.GRAY);
            }
        });

        // ADD TO CART 버튼 눌렀을 때
        cart_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "포토카드가 장바구니에 담겼습니다.", Toast.LENGTH_SHORT).show();
                cart_count.setImageResource(R.drawable.round);
            }
        });

        return view;
    }

    private void requestGet() throws ParseException {
        // photo_seq 값으로 포토카드 검색
        Call<CardData> call = api.getCard(seq);
        call.enqueue(new Callback<CardData>() {
            @Override
            public void onResponse(Call<CardData> call, Response<CardData> response) {
                String uid, id, video, photo, phrase, date, pwd, url;
                int photo_seq, design;

                date = response.body().getPhoto_date();
                id = response.body().getPhoto_id();
                phrase = response.body().getPhrase();
                pwd = response.body().getPhoto_pwd();

                // 날짜 넣어주기
                card_date.setText(date);

                // 이미지 넣어주기
                photo_url = response.body().getPhoto_url();

                // 문구 넣어주기
                tv_phrases.setText(phrase);

                // 아이디 넣어주기
                card_id.setText(id);

                // 이미지 불러오기
                Glide.with(getContext()).load(photo_url).into(cardImageView);

                // 비밀번호 있으면 잠긴 좌물쇠로 이미지 교체
                if ("".equals(pwd)) {
                    lock.setImageResource(R.drawable.lock_open);
                } else{
                    lock.setImageResource(R.drawable.lock);
                }
            }

            @Override
            public void onFailure(Call<CardData> call, Throwable t) {
            }
        });
    }

}
