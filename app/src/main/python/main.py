import cv2
import pickle
import cvzone
import numpy as np

cap = cv2.VideoCapture('car_park_self.mp4')
width, height = 103, 43
#with open('polygons', 'rb') as f:
   # posList = pickle.load(f)
with open('CarParkPos', 'rb') as f:
    posList = pickle.load(f)

img = cv2.imread('car_park_img_self.jpg')
scale_percent = 30
width1 = int(img.shape[1] * scale_percent / 100)
height1 = int(img.shape[0] * scale_percent / 100)

def empty(a):
    pass


cv2.namedWindow("Vals")
cv2.resizeWindow("Vals", 640, 240)
cv2.createTrackbar("Val1", "Vals", 25, 50, empty)
cv2.createTrackbar("Val2", "Vals", 16, 50, empty)
cv2.createTrackbar("Val3", "Vals", 5, 50, empty)


def checkSpaces():
    spaces = 0
    pos_idx = 0
    pos_bits = 0

    for pos in posList:
        x, y = pos
        w, h = width, height

        imgCrop = imgThres[y:y + h, x:x + w]
        count = cv2.countNonZero(imgCrop)

        if count < 350:
            color = (0, 200, 0)
            thic = 5
            spaces += 1
            pos_bits = ((1 << pos_idx) | pos_bits)

        else:
            color = (0, 0, 200)
            thic = 2
        pos_idx += 1
        cv2.rectangle(img, (x, y), (x + w, y + h), color, thic)

        cv2.putText(img, str(cv2.countNonZero(imgCrop)), (x, y + h - 6), cv2.FONT_HERSHEY_PLAIN, 1,
                    color, 2)

    #cvzone.putTextRect(img, f'Free: {spaces}/{len(posList)}', (50, 60), thickness=3, offset=20,
                  #     colorR=(0, 200, 0))
    #file1 = open("parking_bits.txt", "w+")
    #file1.write(bin(pos_bits))
    #file1.close()
    cvzone.putTextRect(img, f'pos_bits: {pos_bits}', (50, 60), thickness=3, offset=20,
                       colorR=(0, 200, 0))
    return pos_bits


while True:

    # Get image frame
    success, img = cap.read()
    img = cv2.resize(img, (width1, height1))
    #cv2.imwrite('car_park_img_self_2.jpg', img)
    if cap.get(cv2.CAP_PROP_POS_FRAMES) == cap.get(cv2.CAP_PROP_FRAME_COUNT):
        cap.set(cv2.CAP_PROP_POS_FRAMES, 0)
    # img = cv2.imread('img.png')
    imgGray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    imgBlur = cv2.GaussianBlur(imgGray, (1, 1), 1)
    # ret, imgThres = cv2.threshold(imgBlur, 150, 255, cv2.THRESH_BINARY)

    val1 = cv2.getTrackbarPos("Val1", "Vals")
    val2 = cv2.getTrackbarPos("Val2", "Vals")
    val3 = cv2.getTrackbarPos("Val3", "Vals")
    if val1 % 2 == 0: val1 += 1
    if val3 % 2 == 0: val3 += 1
    imgThres = cv2.adaptiveThreshold(imgBlur, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
                                     cv2.THRESH_BINARY_INV, val1, val2)
    imgThres = cv2.medianBlur(imgThres, val3)
    kernel = np.ones((3, 3), np.uint8)
    imgThres = cv2.dilate(imgThres, kernel, iterations=1)

    checkSpaces()
    # Display Output
    #cv2.imwrite('car_park_detected.jpg', img)
    cv2.imshow("Image", img)
    # cv2.imshow("ImageGray", imgThres)
    # cv2.imshow("ImageBlur", imgBlur)
    key = cv2.waitKey(60)
    if key == ord('r'):
        pass