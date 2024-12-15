(ns heraldicon.heraldry.escutcheon
  (:require
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.localization.string :as string]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.path :as path]))

(def ^:private heraldicon-attribution
  {:nature :own-work
   :license :public-domain
   :creator-name "Heraldicon"
   :creator-link "https://heraldicon.org"})

(def ^:private heater
  {;; sqrt(3) / 2 * 6 ~ 5.196152422706632
   :shape (str "m 0,0"
               "h 3"
               "v 2"
               "a 6 6 0 0 1 -3,5.196152422706632"
               "a 6 6 0 0 1 -3,-5.196152422706632"
               "v -2"
               "z")

   ::name :string.escutcheon.type/heater
   ::attribution heraldicon-attribution})

(def ^:private square-french
  {:shape (str "m 0,0"
               "v 15.7"
               "c 0,6 6,12 12,13"
               "c 6,-1 12,-7 12,-13"
               "V 0"
               "z")

   ::name :string.escutcheon.type/square-french
   ::attribution heraldicon-attribution})

(def ^:private square-iberian
  {:shape (str "m 0,0"
               "h 5"
               "v 7"
               "a 5 5 0 0 1 -10,0"
               "v -7"
               "z")

   ::name :string.escutcheon.type/square-iberian
   ::attribution heraldicon-attribution})

(def ^:private square-czech
  {;; diff(sqrt(r*r - x*x))
   ;; solve(-24/sqrt(r^2 - 24^2) - (-35/4)) ~ 24.156226
   :shape (str "m 0,0"
               "h 56"
               "l -4,35"
               "a 24.156226 24.156226 0 0 1 -48,0"
               "z")

   ::name :string.escutcheon.type/square-czech
   ::attribution heraldicon-attribution})

(def ^:private french-modern
  {:shape (str "m 0,0"
               "h 7"
               "v 15"
               "a 1 1 0 0 1 -1,1"
               "h -5"
               "a 1 1 0 0 0 -1,1"
               "a 1 1 0 0 0 -1,-1"
               "h -5"
               "a 1 1 0 0 1 -1,-1"
               "v -15"
               "h 7"
               "z")
   :bounding-box (bb/BoundingBox. -7 7 0 16)
   :center-fess? true

   ::name :string.escutcheon.type/french-modern
   ::attribution heraldicon-attribution})

(def ^:private lozenge
  {:shape (str "m 0,0"
               "L 5,6.5"
               "L 0,13"
               "L -5,6.5"
               "z")
   :center-fess? true

   ::name :string.escutcheon.type/lozenge
   ::attribution heraldicon-attribution})

(def ^:private oval
  {:shape (str "m 0,0"
               "A 5 6.8 0 0 1 5,6.5"
               "A 5 6.8 0 0 1 0,13"
               "A 5 6.8 0 0 1 -5,6.5"
               "A 5 6.8 0 0 1 0,0"
               "z")
   :center-fess? true

   ::name :string.escutcheon.type/oval
   ::attribution heraldicon-attribution})

(def ^:private roundel
  {:shape (str "m 0,0"
               "a 5 5 0 0 1 0,10"
               "a 5 5 0 0 1 0,-10"
               "z")
   :center-fess? true

   ::name :string.escutcheon.type/roundel
   ::attribution heraldicon-attribution})

(def ^:private swiss
  {;; sqrt(3) / 2 * 6 ~ 5.196152422706632
   :shape (str "m 0,0"
               "a 6 6 0 0 0 3,0"
               "v 2"
               "a 6 6 0 0 1 -3,5.196152422706632"
               "a 6 6 0 0 1 -3,-5.196152422706632"
               "v -2"
               "a 6 6 0 0 0 3,0"
               "z")

   ::name :string.escutcheon.type/swiss
   ::attribution heraldicon-attribution})

(def ^:private english
  {:shape (str "m 0,0"
               "h 8"
               "a 1 1 0 0 0 -1,1"
               "v 14"
               "a 1 1 0 0 1 -1,1"
               "h -5"
               "a 1 1 0 0 0 -1,1"
               "a 1 1 0 0 0 -1,-1"
               "h -5"
               "a 1 1 0 0 1 -1,-1"
               "v -14"
               "a 1 1 0 0 0 -1,-1"
               "h 8"
               "z")
   :bounding-box (bb/BoundingBox. -7 7 0 16)
   :center-fess? true

   ::name :string.escutcheon.type/english
   ::attribution heraldicon-attribution})

(def ^:private polish
  {:shape "m 43.402145,5e-7
           c -8.662508,0 -14.063932,7.322064 -27.53457,9.380727 0.01086,7.9371285
           -3.321499,15.7448405 -7.7644202,20.8881635 0,0 8.6550412,4.035941
           8.6550412,12.967045 0,13.48538 -14.3402146,13.50873 -14.3402146,13.50873
           0,0 -2.4179809,4.962539 -2.4179809,15.009696 0,22.996861
           15.7236635,40.377428 27.6621895,45.737558 11.938525,5.36013
           18.80961,7.63894 22.359194,12.50808 3.549585,-4.86914 10.377904,-7.14795
           22.316426,-12.50808 11.938526,-5.36013 27.662185,-22.742701
           27.662185,-45.737557 0,-10.047158 -2.41798,-15.009697
           -2.41798,-15.009697 0,0 -14.340209,-0.02335 -14.340209,-13.50873
           0,-8.931104 8.655042,-12.967045 8.655042,-12.967045
           C 87.453242,25.123567 84.122242,17.317856 84.132428,9.3807275
           70.661111,7.3213975 65.259687,5.0000001e-7 56.597858,5.0000001e-7
           51.658715,5.0000001e-7 50.021384,2.5016165 50.021384,2.5016165
           c 0,0 -1.680096,-2.50161599999999 -6.619239,-2.501616 z"
   :bounding-box (bb/BoundingBox. 0 100 2.5016165 130)

   ::name :string.escutcheon.type/polish
   ::attribution (merge heraldicon-attribution
                        {:nature :derivative
                         :source-license :public-domain
                         :source-link "https://commons.wikimedia.org/wiki/File:Polish_Escutcheon.svg"
                         :source-name "Polish Escutcheon"
                         :source-creator-name "Masur"
                         :source-creator-link "https://commons.wikimedia.org/wiki/User:Masur"})})

(def ^:private polish-19th-century
  {:shape "M 9.5659013,0 0,9.9320525 C 13.878078,26.565127 6.4209004,51.856025 6.038075,76.190404
           5.7054978,97.330757 24.490898,120 49.999999,120 75.509101,120 94.294503,97.330757 93.961926,76.190404
           93.579101,51.856025 86.121923,26.565128 100,9.9320525 L 90.434099,0
           C 81.097132,12.169832 64.97276,3.8094077 49.999999,3.8094077
           35.027237,3.8094077 18.90287,12.169832 9.5659013,0 Z"
   :bounding-box (bb/BoundingBox. 0 100 0.3 120)

   ::name :string.escutcheon.type/polish-19th-century
   ::attribution (merge heraldicon-attribution
                        {:nature :derivative
                         :source-license :public-domain
                         :source-link "https://commons.wikimedia.org/wiki/File:HerbTarczaPL_XIXc.svg"
                         :source-name "HerbTarczaPL XIXc"
                         :source-creator-name "NalesnikLD"
                         :source-creator-link "https://commons.wikimedia.org/wiki/User:NalesnikLD"})})

(def ^:private renaissance
  {:shape "M 43.672061,112.35743 C 20.076921,107.21428 1.2267205,96.616647 5.1084778e-7,62.761658
           9.9757105,57.299078 13.336031,27.805689 3.0804505,12.948849 l 5.98179,-9.3387997
           C 28.967341,6.8985193 35.708501,-4.5443607 50,2.1304593 c 14.2915,-6.67482
           21.03266,4.76806 40.93775,1.47959 l 5.9818,9.3387997 C 86.66397,27.805689 90.02428,57.299078
           100,62.761658 98.77327,96.616647 79.92307,107.21428 56.32792,112.35743 51.60688,113.38653
           51.68278,114.71878 50,117 c -1.68279,-2.28122 -1.60689,-3.61347 -6.327939,-4.64257 z"
   :bounding-box (bb/BoundingBox. 0 100 2.1304593 113)

   ::name :string.escutcheon.type/renaissance
   ::attribution (merge heraldicon-attribution
                        {:nature :derivative
                         :source-license :public-domain
                         :source-link "https://commons.wikimedia.org/wiki/File:Coa_Illustration_Shield_Renaissance_7.svg"
                         :source-name "Coa Illustration Shield Renaissance 7"
                         :source-creator-name "Doc Taxon"
                         :source-creator-link "https://commons.wikimedia.org/wiki/User:Doc_Taxon"})})

(def ^:private rectangle
  {:shape "M 0,0 h 10 v 12 h -10 z"
   :center-fess? true

   ::name :string.escutcheon.type/rectangle
   ::attribution heraldicon-attribution})

(def ^:private flag
  {:function (fn [width height swallow-tail tail-point-height tail-tongue]
               (let [swallow-tail-point-dx (-> width (* swallow-tail) (/ 100))
                     dy (-> height (* tail-point-height) (/ 100))
                     half-dy (/ dy 2)
                     swallow-tail-point-top-dist (-> (/ height 2)
                                                     (- half-dy))
                     tail-tongue-point-dx (-> swallow-tail-point-dx
                                              (* tail-tongue)
                                              (/ 100))]
                 {:shape (str
                          "M 0,0"
                          "h " width
                          "l " (- swallow-tail-point-dx) ", " swallow-tail-point-top-dist
                          "l " tail-tongue-point-dx ", " half-dy
                          "l " (- tail-tongue-point-dx) ", " half-dy
                          "l " swallow-tail-point-dx ", " swallow-tail-point-top-dist
                          "h " (- width)
                          "z")
                  :bounding-box (bb/BoundingBox. 0 width 0 height)
                  :points {:fess (v/Vector. (/ width 2) (/ height 2))}}))
   :shape "M 0,0 h 5 v 3 h -5 z"
   :center-fess? true

   ::name :string.escutcheon.type/flag
   ::attribution heraldicon-attribution})

(def ^:private wedge
  {;; sqrt(3) / 2 * 6 + 2 ~ 7.196152422706632
   :shape (let [height 7.196152422706632
                hole-x 1
                hole-y 0.75]
            (str "m 0,0"
                 "h 2"
                 "a " hole-x " " hole-y " 0 0 0 " hole-x "," hole-y
                 "a 8 8 0 0 1 -3," (- height hole-y)
                 "a 8 8 0 0 1 -3," (- (- height hole-y))
                 "a " hole-x " " hole-y " 0 0 0 " hole-x "," (- hole-y)
                 "z"))

   ::name :string.escutcheon.type/wedge
   ::attribution heraldicon-attribution})

(def ^:private kite
  (let [width 1
        height 2
        half-width (/ width 2)
        dx (- half-width (/ half-width 50))
        dy (-> (* half-width
                  half-width)
               (- (* dx dx))
               Math/sqrt)
        R (-> (/ 1 4)
              (+ (* (/ (- height half-width) width)
                    (/ (- height half-width) width)))
              (* width)
              ;; this factor is guessed, based on the 50 above, they
              ;; result in roughly a curve that seems to have no corner
              (* 2))]
    {:shape (str "m 0,0"
                 "a " half-width " " half-width " 0 0 1 " dx " " (+ half-width dy)
                 "a " R " " R " 0 0 1 " (- dx) " " (- height half-width dy)
                 "a " R " " R " 0 0 1 " (- dx) " " (- (- height half-width dy))
                 "a " half-width " " half-width " 0 0 1 " dx " " (- (+ half-width dy))
                 "z")
     :points {:fess (v/Vector. 0 half-width)}

     ::name :string.escutcheon.type/kite
     ::attribution (merge heraldicon-attribution
                          {:nature :derivative
                           :source-license :public-domain
                           :source-link "https://commons.wikimedia.org/wiki/File:Kite_shield.svg"
                           :source-name "Kite shield"
                           :source-creator-name "Perhelion"
                           :source-creator-link "https://commons.wikimedia.org/wiki/User:Perhelion"})}))

(def ^:private norman
  (let [width 5
        height 8
        half-width (/ width 2)
        R (-> (/ 1 4)
              (+ (* (/ height width)
                    (/ height width)))
              (* width))]
    {:shape (str "m 0,0"
                 "h " half-width
                 "a " R " " R " 0 0 1 " (- half-width) " " height
                 "a " R " " R " 0 0 1 " (- half-width) " " (- height)
                 "z")

     ::name :string.escutcheon.type/norman-late
     ::attribution heraldicon-attribution}))

(def ^:private norman-rounded
  (let [width 5
        height 8
        d (/ height 15)
        half-width (/ width 2)
        r (-> (* d d)
              (+ (* half-width half-width))
              (/ 2 d))
        R (-> (/ 1 4)
              (+ (* (/ (- height d) width)
                    (/ (- height d) width)))
              (* width))]
    {:shape (str "m 0,0"
                 "a " r " " r " 0 0 1 " half-width " " d
                 "a " R " " R " 0 0 1 " (- half-width) " " (- height d)
                 "a " R " " R " 0 0 1 " (- half-width) " " (- (- height d))
                 "a " r " " r " 0 0 1 " half-width " " (- d)
                 "z")
     :bounding-box (bb/BoundingBox. (- half-width) half-width d height)

     ::name :string.escutcheon.type/rounded-norman-late
     ::attribution heraldicon-attribution}))

(def ^:private community-square-iberian-engrailed
  {:shape "M 0,0
           a 250,250 0 0 0 250,0
           a 250,250 0 0 0 250,0
           v 350
           a 250,250 0 0 1 -250,250
           a 250,250 0 0 1 -250,-250
           z"
   :bounding-box (bb/BoundingBox. 0 500 33.333333 600)

   ::name "Square Iberian Engrailed"
   ::attribution (merge heraldicon-attribution
                        {:nature :derivative
                         :source-license :public-domain
                         :source-link "https://heraldicon.org"
                         :source-name "Square Iberian Engrailed"
                         :source-creator-name "coinageFission#1205"
                         :source-creator-link "https://discord.com/channels/272117928298676225/272117928298676225"})})

(def ^:private community-pointy-iberian-engrailed
  {:shape "m 0,0
           v 70
           c -0.125,20 5,40 30,45 20,5 20,10 20,10 0,0 0,-5 20,-10 25,-5 30,-25 30,-45
           V 0
           C 100,0 90,5 75,5 60,5 50,0 50,0 50,0 40,5 25,5 10,5 0,0 0,0
           Z"

   ::name "Pointy Iberian Engrailed"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-kalasag
  {:shape "M 54.303084,263.03992 c 0.481806,0.77857 13.294025,21.48369
           13.294025,21.48369 1.495301,2.41641 21.449132,2.28517
           20.298782,-2.22508 C 81.70257,258.01944 90.593797,199.39078
           91.918145,151.87564 93.242492,104.35792 89.3215,28.973867
           100,9.0516574 L 90.936994,0 76.58444,13.160936 C
           96.580416,49.516131 39.329083,48.993572 59.995667,12.50895 L
           50,2.5406234 40.004333,12.50895 C 60.670917,48.993624
           3.4195838,49.516182 23.41556,13.160936 L 9.0630062,0
           0,9.0516574 C 10.678139,28.974383 6.7571471,104.35534
           8.0818554,151.87564 c 1.3243472,47.51772 10.2154196,106.14689
           4.0222536,130.42289 -1.150505,4.51025 18.803326,4.64149
           20.298782,2.22508 0,0 12.812219,-20.70476 13.294025,-21.48369
           0.481791,-0.77863 2.691407,-2.02849 4.303755,-2.02849
           1.612347,0 3.821948,1.24991 4.303754,2.02849 z"
   :bounding-box (bb/BoundingBox. 7.984 (- 100 7.984) 40.25 261)
   :center-fess? true

   ::name "Kalasag"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "vairy"
                  :creator-link "https://heraldicon.org/users/vairy"}})

(def ^:private community-louisiana
  {:shape "M 1.64826,87.810447 C 4.46558,85.281706 7.08513,82.261304
           7.06447,76.558245 L 6.90748,33.595381 C 6.87368,24.346329
           4.63582,22.134663 0,17.219029 4.3479,13.111909
           7.94048,8.4189856 11.55175,3.4256966 21.25678,10.980354
           40.09166,9.5735346 50,0 59.90834,9.5733441 78.74322,10.980164
           88.44825,3.4256966 92.05961,8.4189856 95.65248,13.112
           100,17.219029 c -4.63577,4.915634 -6.87367,7.1273
           -6.90748,16.376352 l -0.15699,42.962864 c -0.0208,5.703059
           2.5987,8.72332 5.41621,11.252202 C 87.51416,108.53152
           61.422,95.64892 49.99796,110 38.5758,95.64892
           12.48364,108.53128 1.64419,87.810447 z"
   :bounding-box (bb/BoundingBox. 3.5 (- 100 3.5) 8.3 101.5)
   :center-fess? true

   ::name "Louisiana"
   ::attribution {:nature :derivative
                  :license :public-domain
                  :creator-name "vairy"
                  :creator-link "https://heraldicon.org/users/vairy"
                  :source-license :public-domain
                  :source-link "https://en.wikipedia.org/wiki/File:Louisiana_state_coat_of_arms_(illustrated,_1876).jpg"
                  :source-name "Louisiana state coat of arms (illustrated, 1876)"
                  :source-creator-name "Godot13"
                  :source-creator-link "https://commons.wikimedia.org/wiki/User:Godot13"}})

(def ^:private community-french-slim
  {:shape "m 0,0 c 0,0 0,100 0,100 0,13.84023 8.933,24.42543 22.1005,25 13.1676,0.39847
           20.7982,1.73968 27.8035,13.02397 7.0052,-11.28472 14.6354,-12.62588 27.8034,-13.02397
           C 90.9714,125 100,113.84023 100,100 V 0 Z"
   :bounding-box (bb/BoundingBox. 0 100 0 125)
   :center-fess? true

   ::name "French Slim"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "vairy"
                  :creator-link "https://heraldicon.org/users/vairy"}})

(def ^:private community-innsbruck
  {:shape "M 91.010471,0.47464845 C 66.714491,-1.6621576 0.810543,2.9581254
           0,23.749 25.532311,4.7047164 29.708341,35.4882 5.304801,30.89755
           c 3.7562,2.19452 2.60989,8.7176 0.99465,22.11063 -5.305876,43.99443
           38.6147,49.50558 42.60417,68.99182 13.01009,-22.07717 49.70929,-24.11915
           51.0587,-69.15807 0.513209,-17.12945 -4.30604,-41.002575 -8.95185,-52.36728155 z"

   ::name "Innsbruck"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "vairy"
                  :creator-link "https://heraldicon.org/users/vairy"}})

(def ^:private community-heater-bulgy
  {:shape "m 50,0 c 10,0 20,0 40,5 5,30 10,52 10,70 0,30 -25,45 -50,60
           C 25,120 0,105 0,75 0,57 5,35 10,5 30,0 40,0 50,0 Z"
   :bounding-box (bb/BoundingBox. 0 100 5 135)

   ::name "Bulgy Heater"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-heater-rounded
  {:shape "m 50,0 c 30,0 50,5 50,5 v 34 c 1.6e-4,35.726721 -19.059752,68.13664 -50,86
           C 19.059752,107.13664 -1.624e-4,74.726721 0,39 V 5 C 0,5 20,0 50,0 Z"
   :bounding-box (bb/BoundingBox. 0 100 5 125)

   ::name "Rounded Heater"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-heater-pointed
  {:shape "m 50,0 c 30,5 50,5 50,5 V 39 C 99.78125,75 80.940248,107.13664 50,125 19.059752,107.13664 0,75 0,39
           V 5 C 0,5 20,5 50,0 Z"
   :bounding-box (bb/BoundingBox. 0 100 5 125)

   ::name "Pointed Heater"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-angular
  {:shape "M 0,5 50,0 100,5 95,85 50,125 5,85 z"
   :bounding-box (bb/BoundingBox. 0 100 5 125)

   ::name "Angular"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-italian-redux
  {:shape "M 50,0
           C 35,0 25,10 25,10 15,20 0,20 0,20
           c 0,55 5,64 10,70 5,6 15,20 15,30 0,30 25,30 25,30 0,0 25,0 25,-30 0,-10 10,-24 15,-30 5,-6 10,-15 10,-70 0,0 -15,0 -25,-10
           C 75,10 65,0 50,0
           Z"

   ::name "Italian Redux"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-extravagant
  {:shape "M 10,85
           C 10,65 0,50 0,50 0,50 10,35 10,25 10,15 5,5 5,5
           H 15
           C 25,5 35,0 40,0 45,0 50,5 50,5 50,5 55,0 60,0 65,0 75,5 85,5
           h 10
           c 0,0 -5,10 -5,20 0,10 10,25 10,25 0,0 -10,15 -10,35 0,25 -40,35 -40,35 0,0 -40,-10 -40,-35
           z"

   ::name "Extravagant"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-philippino
  {:shape "M 35,115
           C 20,115 10,105 10,90
           V 45
           C 10,30 0,20 0,20 15,25 25,10 25,10 40,15 50,0 50,0
           c 0,0 10,15 25,10 0,0 10,15 25,10 0,0 -10,10 -10,25
           v 45
           c 0,15 -10,25 -25,25 -10,0 -15,5 -15,5 0,0 -5,-5 -15,-5
           z"
   :center-fess? true

   ::name "Philippino"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-scutum
  {:shape "M 0,15 v 130 c 0,10 30,15 50,15 20,0 50,-5 50,-15
           V 15 C 100,5 66.749793,0 50,0 30,0 0,5 0,15 z"
   :bounding-box (bb/BoundingBox. 0 100 12.5 147.5)
   :center-fess? true

   ::name "Scutum"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-american
  {:shape "M 15,0 0,15 c 0,0 10,5 10,15 0,15 -10,25 -10,50 0,15 5,30
           25,35 15,3 25,10 25,10 0,0 10,-7 25,-10 20,-5 25,-20 25,-35 C
           100,55 90,45 90,30 90,20 100,15 100,15 L 85,0 C 85,0 80,5 70,5
           55,5 50,0 50,0 50,0 45,5 30,5 20,5 15,0 15,0 z"

   ::name "American"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-triangular
  {:shape "M 50,0 C 80,0 100,10 100,10 100,60 80,90 50,120 20,90
           0,60.03607 0,10 0,10 20,0 50,0 z"
   :bounding-box (bb/BoundingBox. 0 100 10 120)
   #_#_:center-fess? true
   :points {:fess (v/Vector. 50 55)}

   ::name "Triangular"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-german
  {:shape "M 0,20 c 7,-5 20,-5 20,7 0,13 -13,13 -20,8 0,0 7,9 5,15 -2,5
           -5,15 -5,30 0,40 40,40 50,40 10,0 50,0 50,-40 C 100,65 97,55
           95,50 91,40 90,35 90,30 90,28 90,8 100,0 70,0 55,5 40,10 30,5
           25,4 15,5 0,7 0,17 0,20 z"
   :center-fess? true

   ::name "German"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-african
  {:shape "M 45,0 C 45,0 0,15 0,80 c 0,65 45,80 45,80 0,0 45,-15 45,-80 C
           90,15 45,0 45,0 z"
   :center-fess? true

   ::name "African"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-simple-kalasag
  {:shape "m 40,170
           c -10,0 -25,-2 -25,-2
           L 0,10
           C 0,10 10,4 20,3 20,3 20,15 30,25 40,15 40,1 40,1 40,1 45,0 50,0 55,0 60,1 60,1 60,1 60,15 70,25 80,15 80,3 80,3
           c 10,1 20,7 20,7
           L 85,168
           c 0,0 -15,2 -25,2 0,0 0,-10 -10,-20 -10,10 -10,20 -10,20
           z"

   ::name "Simple Kalasag"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-italian
  {:shape "M 50,0
           C 35,0 30,5 25,10
           c -10,10 -25,10 -25,10
             0,60 11.223599,65.396698 15,70
             3.776401,4.603302 14.300142,13.36702 15,35
             1.131708,34.98167 15,35 20,35
             5,0 18.868292,-0.0183 20,-35
             0.699858,-21.63298 11.223599,-30.396698 15,-35
             3.776401,-4.603302 15,-10.00006 15,-70
             0,0 -15,0 -25,-10
           C 70,5 65,0 50,0 Z"

   ::name "Italian"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-nrw
  {:shape "M 10,7 C 5,7 0,5 0,5 0,5 2,10 2,15 v 65 c 0,15 13,25 23,30 20,10
           25,15 25,15 0,0 5,-5 25,-15 10,-5 23,-15 23,-30 V 15 C 98,10 100,5
           100,5 100,5 95,7 90,7 80,7 70,0 60,0 50,0 50,5 50,5 50,5 50,0 40,0
           30,0 20,7 10,7 z"
   :bounding-box (bb/BoundingBox. 2 98 7 125)

   ::name "NRW"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-sleek-pointy
  {:shape "m 100,30
           c 0,0 0,70 -50,120
           C 0,100 0,30 0,30 35,25 50,0 50,0
           c 0,0 15,25 50,30
           z"
   :bounding-box (bb/BoundingBox. 0 100 30 150)
   :points {:fess (v/Vector. 50 75)}

   ::name "Sleek Pointy"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-rhombus
  {:shape "M 50,0 L 100,50 L 50,100 L 0,50 z"

   ::name "Rhombus"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Bananasplit1611"}})

(def ^:private community-queen-consort-modern
  {:shape "M 50,0 c 0,0 -9.494151,12.643461 -22.473589,10.987261 0,0 -4.767819,10.401769
           -27.526411,6.654218 0,0 14.196854,35.081974 3.7934578,71.435548 0,0
           16.0784912,1.395807 23.6211622,16.054983 0,0 12.815277,-3.15411
           22.58538,4.86799 9.770103,-8.0221 22.58538,-4.86799 22.58538,-4.86799
           C 80.12805,90.472834 96.206541,89.077027 96.206541,89.077027
           85.803143,52.723453 100,17.641479 100,17.641479 77.24141,21.38903
           72.473589,10.987261 72.473589,10.987261 59.494151,12.643461 50,0 50,0 z"
   :center-fess? true

   ::name "Queen Consort Modern"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Azure"}})

(def ^:private community-queen-consort
  {:shape "M 27.525575,85.776231 C 8.76938,89.100699 1.1734521,78.586662 1.3765338,69.266866
           1.7505414,52.102977 16.672696,31.57301 0,17.721581 l
           5.9022079,-6.29239 C 13.617952,12.076602 15.659962,2.0017698
           27.60419,9.4773365 33.733164,-1.4890056 44.445825,3.1114914
           49.999999,0 55.554174,3.1114914 66.266836,-1.4890056
           72.39581,9.4773365 84.340038,2.0017698 86.382048,12.076602
           94.097791,11.429191 L 100,17.721581 C 83.327303,31.57301
           98.249458,52.102977 98.623465,69.266866 98.826546,78.586662
           91.230619,89.100699 72.474424,85.776231 71.1126,96.076868
           54.053414,94.72803 49.999999,99.999996 45.946585,94.72803
           28.8874,96.076868 27.525575,85.776231 z"
   :center-fess? true

   ::name "Queen Consort"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Azure"}})

(def ^:private community-octolozenge
  {:shape "M 100,50 C 90.735009,54.857758 80.576139,71.934914 83.497312,83.456671
           72.00358,80.528445 54.967999,90.712116 50.121912,100
           45.27583,90.712116 28.240723,80.528491 16.6246,83.456671
           19.423765,71.934914 9.2649914,54.857758 0,50 9.2649914,45.142242
           19.423861,28.065088 16.502686,16.54333 27.996418,19.471604
           45.032001,9.2878843 49.999998,0 54.967999,9.2878843
           72.003104,19.471509 83.497312,16.54333 80.576184,28.065088
           90.735009,45.142242 100,50 z"
   :center-fess? true

   ::name "Octolozenge"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Azure"}})

(def ^:private community-pointy-iberian
  {:shape "m 0,0 v 65 c 0,10 5,40 30,45 20,5 20,10 20,10 0,0 0,-5 20,-10 25,-5
           30,-35 30,-45 V 0 Z"

   ::name "Pointy Iberian"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-playful
  {:shape "m 48,0 c 39.998852,0 47,8 47,8 5.00273,10 5,17 5,27 0,35.375221
           -23.877758,56.959022 -48,65 C 22.000861,90 -2.9600564e-7,74.570288
           -6.6413221e-8,40 0,30 -6.6413221e-8,20 4.9998565,10 4.9998565,10
           18.000861,0 48,0 Z"

   ::name "Playful"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-bavarian
  {:shape "m 50,0 h 50 v 85 c 0,25 -10,35 -35,35 -5,0 -25,0 -30,0 C 10,120 0,110 0,85 V 0 Z"
   :center-fess? true

   ::name "Bavarian"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-berlin
  {:shape "m 0,0 v 70 c 0,10 0.375,29 25,40 20,10 25,15 25,15 0,0 5,-5 25,-15 25,-10 25,-30 25,-40 V 0 Z"

   ::name "Berlin"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-gonfalon
  {:shape "M 100,0
           h -14.2857142857 V 10
           h -14.2857142857 V 0
           h -14.2857142857 V 10
           h -14.2857142857 V 0
           h -14.2857142857 V 10
           h -14.2857142857 V 0
           H 0 v 170 l 13,10 13,-10
           v -10 l 9,5 v 20 l 15,10 15,-10 v -20 l 9,-5 v 10 l 13,10 13,-10 z"
   :bounding-box (bb/BoundingBox. 0 100 10 160)
   :points {:fess (v/Vector. 50 85)}

   ::name "Gonfalon"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "ashoppio"
                  :creator-link "https://heraldicon.org/users/ashoppio"}})

(def ^:private community-italian-2
  {:shape "M 23.333984,0
           C 20.000655,13.259655 16.666649,23.205189 0,36.464844
           c 16.666649,23.204397 23.33333,43.092477 20,66.296876
             13.33332,0 23.333341,6.63055 30,17.23828
             6.666659,-10.60773 16.66668,-17.23828 30,-17.23828
           C 76.66667,79.557321 83.333351,59.669241 100,36.464844
             83.333351,23.205189 79.999345,13.259655 76.666016,0
             54.299346,1.5483358 37.469728,1.7076968 23.333984,0
           Z"

   ::name "Italian 2"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "ashoppio"
                  :creator-link "https://heraldicon.org/users/ashoppio"}})

(def ^:private community-flag-standard
  {:shape "m 0,0 v 30 h 90 c 0,0 10,0 10,-10 0,-9 -10,-10 -10,-10 z"
   :center-fess? true

   ::name "Standard"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-coffin
  {:shape "M 28,0 C 24.95507,0.08426855 24,3 23,5 L 2,50 c -1,2 -2.08426855,4.011223
           -2,5 0,1 0,1 1,5 l 22,95 c 1,4 2,5 4,5 h 46 c 2,0 3,-1 4,-5 L 99,60
           c 1,-4 1,-4 1,-5 0,-1 -1,-3 -2,-5 L 77,5 C 76,3 75,0 72,0 Z"
   :points {:fess (v/Vector. 50 55)}

   ::name "Coffin Shield"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-hexagonal
  {:shape "M 22,0 C 20,0 19,1 18,5 L 1,75 c -1,4 -0.97529642,4.014712 -1,5 0,1 0,1 1,5
           l 17,70 c 1,4 2,5 4,5 h 56 c 2,0 3,-1 4,-5 L 99,85
           c 1,-4 1,-4 1,-5 0,-1 0,-1 -1,-5 L 82,5 C 81,1 80,0 78,0 Z"
   :center-fess? true

   ::name "Hexagonal"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-pavise
  {:shape "M 10.529661,150 H 90 c 2,0 4.791667,0 5,-5 l 5,-120 c 0,-3 -3,-4 -5,-5 L 56,2
           C 54,1 52,0 50,0 48,0 45.992856,1.04049 44,2 L 5,20 c -2,1 -5,2 -5,5 l 5,120
           c 0.2083333,5 3,5 5.529661,5 z"
   :center-fess? true

   ::name "Pavise"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-elven
  {:shape "M 50,0 C 45,5 25.5,15 20,25
           14.5,35 5,50 0,55
           5,60 15,85 20,100
           c 5,15 25,35 30,40 5,-5 25,-25 30,-40
           C 85,85 95,60 100,55
             95,50 85.5,35 80,25
             74.5,15 55,5 50,0
           Z"
   :points {:fess (v/Vector. 50 60)}

   ::name "Elven"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-orcish
  {:shape "m 50,0 c -10,15 -20,15 -25,10 -5,5 -10,10 -25,0
           15,20 10,35 5,40 5,5 15,20 5,35 15,0 15,10 15,15
           5.26516,0.44194 14.125006,0.12502 20,20 0,-5 0,-10 5,-15
           5,5 5,10 5,15 5.874994,-19.87498 14.73484,-19.55806 20,-20
           0,-5 0,-15 15,-15 -10,-15 0,-30 5,-35
           -5,-5 -10,-20 5,-40 -15,10 -20,5 -25,0
           -5,5 -15,5 -25,-10 z"
   :bounding-box (bb/BoundingBox. 5 95 15 105)

   ::name "Orcish"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-orcish2
  {:shape "M 20,120 C 20,45 0,30 0,30 35,40 25,5 25,5 c 0,0 20,5 25,-5 0,0
           0,15 50,10 0,0 -15,10 -10,80 0,0 -15,-5 -20,10 0,0 -5,-10 -25,-10 -20,0 -25,30 -25,30 z"
   :bounding-box (bb/BoundingBox. 15 90 15 90)
   :center-fess? true

   ::name "Orcish2"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-dwarven
  {:shape "M 30,120 10,85 0,15 20,0 h 60 l 20,15 -10,70 -20,35 z"
   :bounding-box (bb/BoundingBox. 5 95 0 120)
   :points {:fess (v/Vector. 50 55)}

   ::name "Dwarven"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-gnomish
  {:shape "M 100,55 C 100,25 90,0 90,0 10,0 5,10 5,10 c 0,0 -5,15 -5,45 0,45 20,60 25,70 5,-10 75,-15 75,-70 z"
   :bounding-box (bb/BoundingBox. 0 100 5 120)

   ::name "Gnomish"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-halfling
  {:shape "M 50,125 C 20,115 0,75 0,50 0,15 15,0 50,0 c 35,0 50,15 50,50 0,25 -20,65 -50,75 z"
   :bounding-box (bb/BoundingBox. 0 100 0 125)

   ::name "Halfling"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-halfling-pavise
  {:shape "M 20,150 C 5,145 0,75 0,50 0,15 15,0 50,0 c 35,0 50,15 50,50 0,25 -5,95 -20,100 z"
   :bounding-box (bb/BoundingBox. 0 100 0 150)
   :center-fess? true

   ::name "Halfling Pavise"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-owlfolk
  {:shape "M 90,0 C 90,20 100,25 100,60 100,95 60,105 60,105 c 0,0 -5,1 -10,5 -5,-4 -10,-5 -10,-5 C 40,105 0,95 0,60 0,25 10,20 10,0 10,0 25,10 50,10 75,10 90,0 90,0 Z"
   :bounding-box (bb/BoundingBox. 5 95 5 106)
   :center-fess? true

   ::name "Owlfolk"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-arrowhead
  {:shape "m 50,5 c 30,0 45,-5 45,-5 0,0 5,10 5,40 0,30 -20,65 -50,85
           C 20,105 0,70 0,40 0,10 5,0 5,0 5,0 20,5 50,5 Z"
   :bounding-box (bb/BoundingBox. 5 95 0 125)
   :points {:fess (v/Vector. 50 50)}

   ::name "Arrowhead"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-dragonborn
  {:shape "M 30,150 C 30,150 25,95 15,85 5,75 0,80 0,80 0,80 5,70 0,60 0,60 5,50
           0,40 0,40 5,30 0,20 c 0,0 5,5 15,-5 0,0 10,-10 35,-15 25,5 35,15 35,15 10,10
           15,5 15,5 -5,10 0,20 0,20 -5,10 0,20 0,20 -5,10 0,20 0,20 0,0 -5,-5 -15,5
           -10,10 -15,65 -15,65 -10,0 -20,10 -20,10 0,0 -10,-10 -20,-10 z"
   :bounding-box (bb/BoundingBox. 0 100 0 160)

   ::name "Dragonborn"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Korfi2Go"
                  :creator-link "https://heraldicon.org/users/korfi2go"}})

(def ^:private community-waisted-french
  {:shape "M 0,0.26171875
           C 5.777952,17.102504 5.076671,34.052213 0,51.257812 11.131037,106.1402 50,106.02539 50,106.02539
           c 0,0 38.868963,0.11481 50,-54.767578 -5.076671,-17.205599 -5.777952,-34.155308 0,-50.99609325
             -34.290176,2.36055605 -68.179582,2.38739475 -100,0 z"

   ::name "Waisted French"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "Maximilian 'NervousNullPtr' Schleicher"
                  :creator-link "https://github.com/NervousNullPtr"}})

(def ^:private community-manilla
  {:shape "M 39,0 36.054688,10.578125
           H 26.921875
           L 25.966797,3.3671875 8.65625,2.8789062 5.646484,39.802734
           c 4.800686,1.0601 8.28711,4.250504 8.28711,8.025391 4e-6,4.114318 -4.141473,7.540667 -9.611328,8.263672
           L 0,109.12305
           H 0.01172
           C 0.00136,109.36254 0,109.59893 0,109.83984
           c 5e-6,15.96972 22.487329,28.93164 50,28.93164 27.512671,0 49.999995,-12.96192 50,-28.93164 0,-0.24091 -0.0014,-0.4773 -0.01172,-0.71679
           H 100
           L 95.677734,56.091797
           c -5.469855,-0.723005 -9.611332,-4.149354 -9.611328,-8.263672 0,-3.774887 3.486424,-6.965291 8.28711,-8.025391
           L 91.34375,2.8789062 74.033203,3.3671875 73.078125,10.578125
           H 63.945312
           L 61,0
           H 50
           Z"

   ::name "Manilla"
   ::attribution {:nature :derivative
                  :license :public-domain
                  :creator-name "verden"
                  :creator-link "https://heraldicon.org/users/verden"
                  :source-license :public-domain
                  :source-link "https://commons.wikimedia.org/wiki/File:Ph_seal_ncr_manila.svg"
                  :source-name "Ph seal ncr manila"
                  :source-creator-name "chris"
                  :source-creator-link "https://commons.wikimedia.org/wiki/User:Chrkl"}})

(def ^:private community-persian
  {:shape "M 50.010055,0
           A 50.569202,60 0 0 0 0.00414029,51.219922 7.6646945,9.5015312 0 0 1
           2.8045472,50.482031 7.6646945,9.5015312 0 0 1 10.469192,59.983594
           7.6646945,9.5015312 0 0 1 2.8045472,69.485157 7.6646945,9.5015312 0 0
           1 0,68.744727 50.569202,60 0 0 0 50.010055,120 50.569202,60 0 0 0
           99.990537,68.942579 7.6646945,9.5015312 0 0 1 97.490004,69.485157
           7.6646945,9.5015312 0 0 1 89.825359,59.983594 7.6646945,9.5015312 0 0
           1 97.490004,50.482031 7.6646945,9.5015312 0 0 1 100,51.006055
           50.569202,60 0 0 0 50.010055,0
           Z"
   :center-fess? true

   ::name "Persian"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "verden"
                  :creator-link "https://heraldicon.org/users/verden"}})

(def ^:private community-ninja
  {:shape "M 63,133
           C 63,93 80,70 100,60 95,10 70,5 50,0 30,5 5,10 0,60 20,70 37,93 37,133
           l 13,7
           z"

   ::name "Ninja"
   ::attribution {:nature :own-work
                  :license :public-domain
                  :creator-name "zalension"
                  :creator-link "https://heraldicon.org/users/zalension"}})

(def ^:private escutcheons
  [[:string.escutcheon.group/traditional
    [:heater heater]
    [:community-heater-pointed community-heater-pointed]
    [:community-heater-rounded community-heater-rounded]
    [:square-french square-french]
    [:square-iberian square-iberian]
    [:square-czech square-czech]
    [:french-modern french-modern]
    [:english english]
    [:kite kite]
    [:norman-rounded norman-rounded]
    [:norman norman]]
   [:string.escutcheon.group/shapes
    [:flag flag]
    [:community-flag-standard community-flag-standard]
    [:rectangle rectangle]
    [:lozenge lozenge]
    [:community-rhombus community-rhombus]
    [:roundel roundel]
    [:oval oval]]
   [:string.escutcheon.group/decorative
    [:wedge wedge]
    [:swiss swiss]
    [:community-arrowhead community-arrowhead]
    [:community-nrw community-nrw]
    [:renaissance renaissance]
    [:polish polish]
    [:polish-19th-century polish-19th-century]
    [:community-american community-american]]
   [:string.escutcheon.group/community
    [:community-square-iberian-engrailed community-square-iberian-engrailed]
    [:community-pointy-iberian community-pointy-iberian]
    [:community-pointy-iberian-engrailed community-pointy-iberian-engrailed]
    [:community-berlin community-berlin]
    [:community-angular community-angular]
    [:community-waisted-french community-waisted-french]
    [:community-triangular community-triangular]
    [:community-sleek-pointy community-sleek-pointy]
    [:community-heater-bulgy community-heater-bulgy]
    [:community-bavarian community-bavarian]
    [:community-french-slim community-french-slim]
    [:community-german community-german]
    [:community-innsbruck community-innsbruck]
    [:community-italian community-italian]
    [:community-italian-redux community-italian-redux]
    [:community-italian-2 community-italian-2]
    [:community-coffin community-coffin]
    [:community-hexagonal community-hexagonal]
    [:community-pavise community-pavise]
    [:community-scutum community-scutum]
    [:community-louisiana community-louisiana]
    [:community-queen-consort community-queen-consort]
    [:community-queen-consort-modern community-queen-consort-modern]
    [:community-octolozenge community-octolozenge]
    [:community-ninja community-ninja]
    [:community-dragonborn community-dragonborn]
    [:community-dwarven community-dwarven]
    [:community-elven community-elven]
    [:community-orcish community-orcish]
    [:community-orcish2 community-orcish2]
    [:community-owlfolk community-owlfolk]
    [:community-gnomish community-gnomish]
    [:community-halfling community-halfling]
    [:community-halfling-pavise community-halfling-pavise]
    [:community-african community-african]
    [:community-extravagant community-extravagant]
    [:community-philippino community-philippino]
    [:community-playful community-playful]
    [:community-persian community-persian]
    [:community-manilla community-manilla]
    [:community-gonfalon community-gonfalon]
    [:community-simple-kalasag community-simple-kalasag]
    [:community-kalasag community-kalasag]]])

(def ^:private kinds-map
  (into {}
        (for [[_ & items] escutcheons
              [key value] items]
          [key value])))

(def choices
  (into [[:string.escutcheon.type/none
          [:string.escutcheon.type/none :none]]]
        (map (fn [[group-name & items]]
               (into [group-name]
                     (map (fn [[key data]]
                            [(::name data) key]))
                     items)))
        escutcheons))

(def escutcheon-map
  (options/choices->map choices))

(defn attribution [escutcheon]
  (let [{::keys [attribution name]} (get kinds-map escutcheon)]
    (assoc attribution :name name)))

(defn data [escutcheon-type flag-width flag-height swallow-tail tail-point-height tail-tongue]
  (let [escutcheon-data (get kinds-map escutcheon-type)
        {:keys [bounding-box
                points
                shape
                center-fess?]} (if (= escutcheon-type :flag)
                                 ((:function escutcheon-data) flag-width flag-height swallow-tail tail-point-height tail-tongue)
                                 escutcheon-data)
        bounding-box (or bounding-box (bb/from-paths [shape]))
        points (cond-> points
                 center-fess? (assoc :fess (bb/center bounding-box)))]
    {:shape shape
     :bounding-box bounding-box
     :environment (environment/create bounding-box points :root? true)}))

(defn transform-to-width [{:keys [shape environment]} target-width]
  (let [width (:width environment)
        original-shape-bounding-box (bb/from-paths [shape])
        top-left-shape (bb/top-left original-shape-bounding-box)
        effective-offset (v/sub top-left-shape)
        scale-factor (/ target-width width)
        transform-bb (fn [bb]
                       (-> bb
                           (bb/translate effective-offset)
                           (bb/scale scale-factor)))
        transformed-shape (-> shape
                              path/parse-path
                              (path/translate (:x effective-offset) (:y effective-offset))
                              (path/scale scale-factor scale-factor)
                              path/to-svg)
        shape-bounding-box (bb/from-paths [transformed-shape])]
    {:shape transformed-shape
     :shape-bounding-box shape-bounding-box
     :environment (-> environment
                      (update :width * scale-factor)
                      (update :height * scale-factor)
                      (update :points (fn [points]
                                        (into {}
                                              (map (fn [[key value]]
                                                     [key (-> value
                                                              (v/add effective-offset)
                                                              (v/mul scale-factor))]))
                                              points)))
                      (update :bounding-box transform-bb))}))

(def flag-options
  {:flag-aspect-ratio-preset {:type :option.type/choice
                              :choices [[(string/str-tr "--- " :string.escutcheon.type/select-ratio " ---") :none]
                                        ["2:3 (most common)" :preset-2-3]
                                        ["1:2 (common)" :preset-1-2]
                                        ["3:5 (common)" :preset-3-5]
                                        ["1:1" :preset-1-1]
                                        ["3:4" :preset-3-4]
                                        ["4:3" :preset-4-3]
                                        ["4:5" :preset-4-5]
                                        ["4:7" :preset-4-7]
                                        ["5:7" :preset-5-7]
                                        ["5:8" :preset-5-8]
                                        ["6:7" :preset-6-7]
                                        ["6:13" :preset-6-13]
                                        ["7:10" :preset-7-10]
                                        ["7:11" :preset-7-11]
                                        ["7:13" :preset-7-13]
                                        ["7:17" :preset-7-17]
                                        ["8:11" :preset-8-11]
                                        ["9:16" :preset-9-16]
                                        ["10:17" :preset-10-17]
                                        ["10:19" :preset-10-19]
                                        ["11:18" :preset-11-18]
                                        ["11:19" :preset-11-19]
                                        ["11:20" :preset-11-20]
                                        ["11:28" :preset-11-28]
                                        ["13:15" :preset-13-15]
                                        ["15:22" :preset-15-22]
                                        ["16:25" :preset-16-25]
                                        ["16:27" :preset-16-27]
                                        ["17:26" :preset-17-26]
                                        ["18:25" :preset-18-25]
                                        ["19:36" :preset-19-36]
                                        ["22:41" :preset-22-41]
                                        ["28:37" :preset-28-37]]
                              :ui/label :string.option/aspect-ratio-preset
                              :ui/element :ui.element/flag-aspect-ratio-preset-select}

   :flag-width {:type :option.type/range
                :default 3
                :min 0.1
                :max 41
                :ui/label :string.option/flag-width
                :ui/step 0.01}

   :flag-height {:type :option.type/range
                 :default 2
                 :min 0.1
                 :max 41
                 :ui/label :string.option/flag-height
                 :ui/step 0.01}

   :flag-swallow-tail {:type :option.type/range
                       :default 0
                       :min 0
                       :max 100
                       :ui/label :string.option/flag-swallow-tail
                       :ui/step 0.01}

   :flag-tail-point-height {:type :option.type/range
                            :default 0
                            :min 0
                            :max 90
                            :ui/label :string.option/flag-tail-point-height
                            :ui/step 0.01}

   :flag-tail-tongue {:type :option.type/range
                      :default 0
                      :min 0
                      :max 100
                      :ui/label :string.option/flag-tongue
                      :ui/step 0.01}})
