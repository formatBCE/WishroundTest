package ua.pp.formatbce.wishroundtest;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Session;
import com.facebook.model.GraphUser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by format on 25.02.2015
 */
public class DataWorker {

    private static final char[] abcCyr = {'à', 'á', 'â', 'ã', 'ä', 'å', '¸', 'æ', 'ç', 'è', 'é', 'ê', 'ë', 'ì', 'í', 'î', 'ï', 'ð', 'ñ', 'ò', 'ó', 'ô', 'õ', 'ö', '÷', 'ø', 'ù', 'û', 'ý', 'þ', 'ÿ', 'ü', 'ú'};
    private static final String[] abcLat = {"a", "b", "v", "g", "d", "e", "e", "zh", "z", "i", "y", "k", "l", "m", "n", "o", "p", "r", "s", "t", "u", "f", "h", "ts", "ch", "sh", "sch", "y", "e", "ju", "ja", "'", ""};


    private List<FBUser> allFriends;
    private List<Contact> contacts;
    private Session session;
    private Activity cnt;
    private OnFBDataCollectedListener listener;

    public DataWorker(@NonNull OnFBDataCollectedListener listener, @NonNull Activity cnt) {
        session = Session.getActiveSession();
        this.cnt = cnt;
        this.listener = listener;
    }

    public void loadData() {
        loadUserData();
    }

    private void compareContacts() {
        listener.setProgress("Searching for match...");
        Set<FBUser> result = new HashSet<>();
        for (FBUser u : allFriends) {
            for (int i = 0; i < contacts.size(); ) {
                Contact c = contacts.get(i);
                if (areTheSame(u, c)) {
                    u.setPhone(c.phone);
                    contacts.remove(c);
                    if (result.add(u)) {
                        listener.setProgress(result.size() + " matches found...");
                    }
                    break;
                } else {
                    i++;
                }
            }
        }
        if (!result.isEmpty()) {
            listener.onDataCollected(result);
        } else {
            listener.onError("No match.");
        }
    }

    private boolean areTheSame(FBUser u, Contact c) {
        return (u.fName.equals(c.fName) && u.lName.equals(c.lName))
                || (u.fName.equals(c.lName) && u.lName.equals(c.fName));
    }

    private static String transliterate(String text) {
        StringBuilder builder = new StringBuilder();
        text = text.toLowerCase();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            for (int x = 0; x < abcCyr.length; x++) {
                if (c == abcCyr[x]) {
                    builder.append(abcLat[x]);
                    break;
                }
                if (x == abcCyr.length - 1) {
                    builder.append(c);
                }
            }
        }
        builder.setCharAt(0, String.valueOf(builder.charAt(0)).toUpperCase().charAt(0));
        return builder.toString();
    }

    private void loadAllContacts() {
        List<Contact> result = new ArrayList<>();
        Cursor phones = cnt.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        while (phones.moveToNext()) {
            String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            Contact c = new Contact(name, phoneNumber);
            if (c.isValid) {
                result.add(c);
            }
        }
        phones.close();
        setContacts(result);
    }

    private void loadUserFriends() {
        listener.setProgress("Loading contacts...");
        Request request = Request.newMyFriendsRequest(session, (graphUsers, response) -> {
            if (graphUsers == null || graphUsers.isEmpty()) {
                listener.onError("Cannot load Facebook friends.");
                Log.e("RESPONSE", response.toString());
            } else {
                Log.e("Graph friends size: ", graphUsers.size() + "");
                List<FBUser> users = new ArrayList<>();
                for (GraphUser gu : graphUsers) {
                    FBUser u = FBUser.create(gu);
                    loadImageUrl(u);
                    users.add(u);
                }
                setAllFriends(users);
            }
        });
        Bundle bundle = request.getParameters();
        bundle.putString("fields", "id,first_name,last_name");
        request.executeAsync();
    }

    private void loadUserData() {
        listener.setProgress("Loading user data...");
        Request.newMeRequest(session, (user, response) -> {
            if (user != null) {
                FBUser u = FBUser.create(user);
                loadImageUrl(u);
                listener.onCurrentUserInfo(u);
                loadUserFriends();
                loadAllContacts();
            } else {
                listener.onError("Cannot load current user data.");
            }
        }).executeAsync();
    }

    private void loadImageUrl(FBUser u) {
        Request imgRequest = new Request(session, "/" + u.id + "/picture", null, HttpMethod.GET,
                response1 -> {
                    try {
                        u.imgLink = response1.getGraphObject().getInnerJSONObject().getJSONObject("data").getString("url");
                        listener.onNewImageUrlReady(u.id);
                    } catch (Exception e) {
                        Log.e("JSON", response1.getGraphObject().getInnerJSONObject().toString());
                        e.printStackTrace();
                    }
                });
        Bundle bundle = imgRequest.getParameters();
        bundle.putBoolean("redirect", false);
        bundle.putString("height", "200");
        bundle.putString("type", "normal");
        bundle.putString("width", "200");
        imgRequest.executeAsync();
    }

    public void setContacts(@NonNull List<Contact> contacts) {
        Log.e("Contacts size", contacts.size() + "");
        this.contacts = contacts;
        if (allFriends != null) {
            compareContacts();
        }
    }

    public void setAllFriends(@NonNull List<FBUser> friends) {
        this.allFriends = friends;
        if (contacts != null) {
            compareContacts();
        }
    }

    public static interface OnFBDataCollectedListener {

        void onCurrentUserInfo(FBUser user);

        void onDataCollected(Set<FBUser> data);

        void setProgress(String whereWeAre);

        void onError(String error);

        void onNewImageUrlReady(String id);
    }

    static class FBUser {
        private String fName = "";
        private String lName = "";
        private String id = "";
        private String imgLink;
        private String phone;

        @Override
        public String toString() {
            return fName + " " + lName;
        }

        @Override
        public int hashCode() {
            return (fName == null ? 13 : fName.hashCode()) + (17 * (lName == null ? 23 : lName.hashCode()));
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof FBUser)) {
                return false;
            }
            FBUser other = (FBUser) o;
            return other.fName.equals(fName) && other.lName.equals(lName);
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }


        public String getFName() {
            return fName;
        }

        public String getLName() {
            return lName;
        }

        public String getImgUrl() {
            return imgLink;
        }

        public String getPhone() {
            return phone;
        }

        public String getId() {
            return id;
        }

        public static FBUser create(GraphUser gu) {
            FBUser user = new FBUser();
            user.fName = transliterate(gu.getFirstName().trim());
            user.lName = transliterate(gu.getLastName().trim());
            user.id = gu.getId();
            return user;
        }

        public static void showInfo(FBUser user, Context cnt) {
            new MaterialDialog.Builder(cnt)
                    .title("User info")
                    .content("First Name: " +
                            user.getFName() + "\nLast Name: " +
                            user.getLName() +
                            (user.getPhone() == null ? "" : ("\nPhone: " +
                                    user.getPhone())))
                    .positiveText("Close").show();
        }
    }

    private static class Contact {
        private static final String PREFIX = "+380";
        private String fName = "";
        private String lName = "";
        private String phone;
        private boolean isValid = true;

        public Contact(String name, String phoneNumber) {
            String[] flName = name.trim().split(" ");
            switch (flName.length) {
                case 1:
                    fName = transliterate(flName[0].trim());
                    break;
                case 2:
                    fName = transliterate(flName[0].trim());
                    lName = transliterate(flName[1].trim());
                    break;
                default:
                    isValid = false;
                    break;
            }
            int length = phoneNumber.length();
            phone = (length < 10 ? phoneNumber : (PREFIX + phoneNumber.substring(length - 9, length))).replace(" ", "");
        }
    }

    public static Bitmap applyCircleEffect(Bitmap src) {
        try {
            // image size
            int width = src.getWidth();
            int height = src.getHeight();
            // create bitmap output
            Bitmap result = Bitmap.createBitmap(width, height,
                    Bitmap.Config.ARGB_8888);
            // set canvas for painting
            Canvas canvas = new Canvas(result);
            canvas.drawARGB(0, 0, 0, 0);

            // config paint
            final Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.BLACK);

            // config rectangle for embedding
            final Rect rect = new Rect(0, 0, width, height);
            final RectF rectF = new RectF(rect);

            // draw rect to canvas
            canvas.drawRoundRect(rectF, width / 2, height / 2, paint);

            // create Xfer mode
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            // draw source image to canvas
            canvas.drawBitmap(src, rect, rect, paint);

            // return final image
            return result;
        } catch (Exception e) {
            return src;
        }
    }
}
