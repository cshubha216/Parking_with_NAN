import cv2
import pickle

width, height = 300, 114

try:
    with open('CarParkPos', 'rb') as f:
        posList = pickle.load(f)
except:
    posList = []


def mouseClick(events, x, y, flags, params):
    if events == cv2.EVENT_LBUTTONDOWN:
        posList.append((x, y))
    if events == cv2.EVENT_RBUTTONDOWN:
        for i, pos in enumerate(posList):
            x1, y1 = pos
            if x1 < x < x1 + width and y1 < y < y1 + height:
                posList.pop(i)

    with open('CarParkPos', 'wb') as f:
        pickle.dump(posList, f)


while True:
    img = cv2.imread('car_park_img_self_2.jpg')
    # percent by which the image is resized
    scale_percent = 100

    # calculate the 50 percent of original dimensions
    width1 = int(img.shape[1] * scale_percent / 100)
    height1 = int(img.shape[0] * scale_percent / 100)

    # dsize
    dsize = (width1, height1)

    # resize image
    img = cv2.resize(img, dsize)
    for pos in posList:
        cv2.rectangle(img, pos, (pos[0] + width, pos[1] + height), (255, 0, 255), 2)

    cv2.imshow("Image", img)
    cv2.setMouseCallback("Image", mouseClick)
    cv2.waitKey(1)