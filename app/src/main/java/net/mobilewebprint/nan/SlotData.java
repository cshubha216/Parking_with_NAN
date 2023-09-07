package net.mobilewebprint.nan;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

public class SlotData implements Serializable {
    String slotId;
    String subId;

    SlotData(String slotId, String subId) {
        this.slotId = slotId;
        this.subId = subId;
    }
}

class Constant {
    static ArrayList<SlotData> slots = new ArrayList();

    public void addSlot(byte[] message) {
        JSONObject jsonData = null;
        try {
            jsonData = new JSONObject(new String(message));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeSlot(byte[] slot) {
    }


    public static ArrayList<SlotData> getStaticSlots() {
        if (!slots.isEmpty()) return slots;
        slots.add(new SlotData("a00", ""));
        slots.add(new SlotData("a01", ""));
        slots.add(new SlotData("a02", ""));
        slots.add(new SlotData("a03", ""));
        slots.add(new SlotData("a04", ""));
        slots.add(new SlotData("a05", ""));
        slots.add(new SlotData("a06", ""));
        slots.add(new SlotData("a07", ""));
        slots.add(new SlotData("a08", ""));
        slots.add(new SlotData("a09", ""));
        return slots;
    }

    public static byte[] getSlotsDataAsBytes() {
        try {
            return objectToBytes(getStaticSlots());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ArrayList<SlotData> convertBytesToSlotsData(byte[] bytes) {
        try {
            ArrayList<SlotData> restoredSlots = bytesToArrayList(bytes);
            slots = restoredSlots;
            return restoredSlots;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void updateSlot(byte[] slot) {
        try {
            ArrayList<SlotData> restoredSlots = bytesToArrayList(slot);
            String slotId= new String(slot, StandardCharsets.UTF_8);
            for (int i = 0; i < slots.size(); i++) {
                if (slotId.equals(slots.get(i).slotId)) {
                    slots.set(i, new SlotData(slotId,"adsa"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public static ArrayList<SlotData> bytesToArrayList(byte[] slot) {
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(slot);
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            ArrayList<SlotData> restoredSlots = (ArrayList<SlotData>) objectInputStream.readObject();
            objectInputStream.close();
            byteArrayInputStream.close();
            return restoredSlots;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] objectToBytes(ArrayList<SlotData> slot) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);

            // Serialize the ArrayList to the output stream
            objectOutputStream.writeObject(slot);
            objectOutputStream.flush();

            // Get the byte array
            byte[] bytes = byteArrayOutputStream.toByteArray();
            // Close streams
            objectOutputStream.close();
            byteArrayOutputStream.close();
            return bytes;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static byte[] anyObjectToBytes(Map<String, String> slot) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);

            // Serialize the ArrayList to the output stream
            objectOutputStream.writeObject(slot);
            objectOutputStream.flush();

            // Get the byte array
            byte[] bytes = byteArrayOutputStream.toByteArray();
            // Close streams
            objectOutputStream.close();
            byteArrayOutputStream.close();
            return bytes;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

