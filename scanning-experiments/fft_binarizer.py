import numpy as np
import matplotlib.image as mpimg
import scipy as sp
from matplotlib import pyplot as plt

img = mpimg.imread("resources/vlcsnap-2016-12-02-11h23m47s301.png")
#r, g, b = [np.array(cim.getdata()).reshape(h, w) for cim in [rim, gim, bim]]
#l = 0.333 * (r + g + b)
l = 1/3.0*np.sum(img, axis=2)

h, w = l.shape

Y = 566
plt.figure()
plt.subplot(221)
plt.plot(l[Y])
#plt.ylim(-20000, 20000)
ft = sp.fft(l[Y])

ft[0:10] = 0
ft[w/2-50:w/2+50] = 0
ft[-10:] = 0
ift = sp.ifft(ft)

plt.subplot(223)
plt.plot(ift)

plt.subplot(222)
lambd = np.linspace(w/2, 2, w/2)
plt.plot(lambd[50:w/2], np.abs(ft[50:w/2]))

img2 = [[], [], []]

#for y in range(h):
#    ftc = [sp.fft(c[y]) for c in [r,g,b]]
#    for ftcx in ftc:
#        ftcx[0:1000] = 0
#    img2.append(list(np.ifft(ftcx)))
img2 = np.zeros((h, w), dtype='bool')
for y in range(h):
    ftc = sp.fft(l[y])
    ftc[0:10] = 0
    ftc[w/2-50:w/2+50] = 0
    ftc[-10:] = 0
    img2[y,:] = ((sp.ifft(ftc)) > 0)

plt.subplot(224)
plt.imshow(img2)

plt.show()



