(ns heraldicon.heraldry.escutcheon
  (:require
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.localization.string :as string]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]))

(def ^:private heraldicon-attribution
  {:nature :own-work
   :license :public-domain
   :username "Heraldicon"})

(def ^:private heater
  {;; sqrt(3) / 2 * 6 ~ 5.196152422706632
   :environment (environment/create
                 (str "m 0,0"
                      "h 3"
                      "v 2"
                      "a 6 6 0 0 1 -3,5.196152422706632"
                      "a 6 6 0 0 1 -3,-5.196152422706632"
                      "v -2"
                      "z")
                 {:context :root
                  :bounding-box (bb/BoundingBox. -3 3 0 (+ 2 5.196152422706632))})
   :attribution heraldicon-attribution})

(def ^:private square-french
  {:environment (environment/create
                 (str "m 0,0"
                      "v 15.7"
                      "c 0,6 6,12 12,13"
                      "c 6,-1 12,-7 12,-13"
                      "V 0"
                      "z")
                 {:context :root
                  :bounding-box (bb/BoundingBox. 0 (* 2 12) 0 (+ 15.7 13))})
   :attribution heraldicon-attribution})

(def ^:private square-iberian
  {:environment (environment/create
                 (str "m 0,0"
                      "h 5"
                      "v 7"
                      "a 5 5 0 0 1 -10,0"
                      "v -7"
                      "z")
                 {:context :root
                  :bounding-box (bb/BoundingBox. -5 5 0 (+ 7 5))})
   :attribution heraldicon-attribution})

(def ^:private square-czech
  {:environment (environment/create
                 ;; diff(sqrt(r*r - x*x))
                 ;; solve(-24/sqrt(r^2 - 24^2) - (-35/4)) ~ 24.156226
                 (str "m 0,0"
                      "h 56"
                      "l -4,35"
                      "a 24.156226 24.156226 0 0 1 -48,0"
                      "z")
                 {:context :root
                  :bounding-box (bb/BoundingBox. 0 56 0 56.5)})
   :attribution heraldicon-attribution})

(def ^:private french-modern
  {:environment (environment/create
                 (str "m 0,0"
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
                 {:context :root
                  :bounding-box (bb/BoundingBox. -7 7 0 16)
                  :width 14
                  :height 17
                  :points {:fess (v/Vector. 0 8)}})
   :attribution heraldicon-attribution})

(def ^:private lozenge
  {:environment (environment/create
                 (str "m 0,0"
                      "L 5,6.5"
                      "L 0,13"
                      "L -5,6.5"
                      "z")
                 {:context :root
                  :bounding-box (bb/BoundingBox. -5 5 0 13)
                  :points {:fess (v/Vector. 0 6.5)}})
   :attribution heraldicon-attribution})

(def ^:private oval
  {:environment (environment/create
                 (str "m 0,0"
                      "A 5 6.8 0 0 1 5,6.5"
                      "A 5 6.8 0 0 1 0,13"
                      "A 5 6.8 0 0 1 -5,6.5"
                      "A 5 6.8 0 0 1 0,0"
                      "z")
                 {:context :root
                  :bounding-box (bb/BoundingBox. -5 5 0 13)
                  :points {:fess (v/Vector. 0 6.5)}})
   :attribution heraldicon-attribution})

(def ^:private roundel
  {:environment (environment/create
                 (str "m 0,0"
                      "a 5 5 0 0 1 0,10"
                      "a 5 5 0 0 1 0,-10"
                      "z")
                 {:context :root
                  :bounding-box (bb/BoundingBox. -5 5 0 10)
                  :points {:fess (v/Vector. 0 5)}})
   :attribution heraldicon-attribution})

(def ^:private swiss
  {;; sqrt(3) / 2 * 6 ~ 5.196152422706632
   :environment (environment/create
                 (str "m 0,0"
                      "a 6 6 0 0 0 3,0"
                      "v 2"
                      "a 6 6 0 0 1 -3,5.196152422706632"
                      "a 6 6 0 0 1 -3,-5.196152422706632"
                      "v -2"
                      "a 6 6 0 0 0 3,0"
                      "z")
                 {:context :root
                  :bounding-box (bb/BoundingBox. -3 3 0 (+ 2 5.196152422706632))})
   :attribution heraldicon-attribution})

(def ^:private english
  {:environment (environment/create
                 (str "m 0,0"
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
                 {:context :root
                  :bounding-box (bb/BoundingBox. -7 7 0 16)
                  :width 16
                  :height 17
                  :offset (v/Vector. -1 0)
                  :points {:fess (v/Vector. 0 8)}})
   :attribution heraldicon-attribution})

(def ^:private polish
  {:environment (environment/create
                 (str "m 43.402145,5e-7 "
                      "c -8.662508,0 -14.063932,7.322064 -27.53457,9.380727 0.01086,7.9371285
           -3.321499,15.7448405 -7.7644202,20.8881635 0,0 8.6550412,4.035941
           8.6550412,12.967045 0,13.48538 -14.3402146,13.50873 -14.3402146,13.50873
           0,0 -2.4179809,4.962539 -2.4179809,15.009696 0,22.996861
           15.7236635,40.377428 27.6621895,45.737558 11.938525,5.36013
           18.80961,7.63894 22.359194,12.50808 3.549585,-4.86914 10.377904,-7.14795
           22.316426,-12.50808 11.938526,-5.36013 27.662185,-22.742701
           27.662185,-45.737557 0,-10.047158 -2.41798,-15.009697
           -2.41798,-15.009697 0,0 -14.340209,-0.02335 -14.340209,-13.50873
           0,-8.931104 8.655042,-12.967045 8.655042,-12.967045 "
                      "C 87.453242,25.123567 84.122242,17.317856 84.132428,9.3807275
           70.661111,7.3213975 65.259687,5.0000001e-7 56.597858,5.0000001e-7
           51.658715,5.0000001e-7 50.021384,2.5016165 50.021384,2.5016165 "
                      "c 0,0 -1.680096,-2.50161599999999 -6.619239,-2.501616 "
                      "z")
                 {:context :root
                  :bounding-box (bb/BoundingBox. 0 100 0 130)
                  :points {:fess (v/Vector. 50 60)}})
   :attribution (merge heraldicon-attribution
                       {:nature :derivative
                        :source-license :public-domain
                        :source-link "https://commons.wikimedia.org/wiki/File:Polish_Escutcheon.svg"
                        :source-name "Polish Escutcheon"
                        :source-creator-name "Masur"
                        :source-creator-link "https://commons.wikimedia.org/wiki/User:Masur"})})

(def ^:private polish-19th-century
  {:environment (environment/create
                 (str
                  "M 9.5919374,7.6420451e-7 6.7196191e-7,9.9320533 "
                  "C 13.91585,26.565128 6.4383768,51.856026 6.0545095,76.190405
       5.7210271,97.330758 24.557556,120 50.136084,120 75.714614,120
       94.551144,97.330758 94.217662,76.190405 93.833795,51.856026
       86.356321,26.565129 100.27217,9.9320533 "
                  "L 90.680234,7.6420451e-7 "
                  "C 81.317854,12.169833 65.149597,3.8094085 50.136084,3.8094085
       35.122571,3.8094085 18.954318,12.169833 9.5919374,7.6420451e-7 "
                  "Z")
                 {:context :root
                  :bounding-box (bb/BoundingBox. 0 100 0 120)
                  :points {:fess (v/Vector. 50 60)}})
   :attribution (merge heraldicon-attribution
                       {:nature :derivative
                        :source-license :public-domain
                        :source-link "https://commons.wikimedia.org/wiki/File:HerbTarczaPL_XIXc.svg"
                        :source-name "HerbTarczaPL XIXc"
                        :source-creator-name "NalesnikLD"
                        :source-creator-link "https://commons.wikimedia.org/wiki/User:NalesnikLD"})})

(def ^:private renaissance
  {:environment (environment/create
                 (str
                  "M 43.672061,112.35743 "
                  "C 20.076921,107.21428 1.2267205,96.616647 5.1084778e-7,62.761658
       9.9757105,57.299078 13.336031,28.673358 3.0804505,13.816518 "
                  "L 9.0622405,3.6100493 "
                  "C 28.967341,6.8985193 35.708501,-4.5443607 50,2.1304593 "
                  "c 14.2915,-6.67482 21.03266,4.76806 40.93775,1.47959 "
                  "l 5.9818,10.2064687 "
                  "C 86.66397,28.673358 90.02428,57.299078 100,62.761658 98.77327,96.616647
       79.92307,107.21428 56.32792,112.35743 51.60688,113.38653 51.68278,114.71878 50,117 "
                  "c -1.68279,-2.28122 -1.60689,-3.61347 -6.327939,-4.64257 "
                  "Z")
                 {:context :root
                  :bounding-box (bb/BoundingBox. 0 100 0 117)
                  :points {:fess (v/Vector. 50 55)}})
   :attribution (merge heraldicon-attribution
                       {:source-license :public-domain
                        :source-link "https://commons.wikimedia.org/wiki/File:Coa_Illustration_Shield_Renaissance_7.svg"
                        :source-name "Coa Illustration Shield Renaissance 7"
                        :source-creator-name "Doc Taxon"
                        :source-creator-link "https://commons.wikimedia.org/wiki/User:Doc_Taxon"})})

(def ^:private rectangle
  {:environment (environment/create
                 (str
                  "M 0,0"
                  "h 10"
                  "v 12"
                  "h -10"
                  "z")
                 {:context :root
                  :bounding-box (bb/BoundingBox. 0 10 0 12)
                  :points {:fess (v/Vector. 5 6)}})
   :attribution heraldicon-attribution})

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
                 (environment/create
                  (str
                   "M 0,0"
                   "h " width
                   "l " (- swallow-tail-point-dx) ", " swallow-tail-point-top-dist
                   "l " tail-tongue-point-dx ", " half-dy
                   "l " (- tail-tongue-point-dx) ", " half-dy
                   "l " swallow-tail-point-dx ", " swallow-tail-point-top-dist
                   "h " (- width)
                   "z")
                  {:context :root
                   :bounding-box (bb/BoundingBox. 0 width 0 height)
                   :points {:fess (v/Vector. (/ width 2) (/ height 2))}})))
   :environment (environment/create
                 (str
                  "M 0,0"
                  "h 5"
                  "v 3"
                  "h -5"
                  "z")
                 {:context :root
                  :bounding-box (bb/BoundingBox. 0 5 0 3)
                  :points {:fess (v/Vector. 2.5 1.5)}})
   :attribution heraldicon-attribution})

(def ^:private wedge
  {;; sqrt(3) / 2 * 6 + 2 ~ 7.196152422706632
   :environment (let [height 7.196152422706632
                      hole-x 1
                      hole-y 0.75]
                  (environment/create
                   (str "m 0,0"
                        "h 2"
                        "a " hole-x " " hole-y " 0 0 0 " hole-x "," hole-y
                        "a 8 8 0 0 1 -3," (- height hole-y)
                        "a 8 8 0 0 1 -3," (- (- height hole-y))
                        "a " hole-x " " hole-y " 0 0 0 " hole-x "," (- hole-y)
                        "z")
                   {:context :root
                    :bounding-box (bb/BoundingBox. -3 3 0 7.196152422706632)}))
   :attribution heraldicon-attribution})

(def ^:private kite
  {:environment (let [width 1
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
                  (environment/create
                   (str "m 0,0"
                        "a " half-width " " half-width " 0 0 1 " dx " " (+ half-width dy)
                        "a " R " " R " 0 0 1 " (- dx) " " (- height half-width dy)
                        "a " R " " R " 0 0 1 " (- dx) " " (- (- height half-width dy))
                        "a " half-width " " half-width " 0 0 1 " dx " " (- (+ half-width dy))
                        "z")
                   {:context :root
                    :bounding-box (bb/BoundingBox. (- half-width) half-width 0 height)
                    :points {:fess (v/Vector. 0 half-width)}}))
   :attribution (merge heraldicon-attribution
                       {:source-license :public-domain
                        :source-link "https://commons.wikimedia.org/wiki/File:Kite_shield.svg"
                        :source-name "Kite shield"
                        :source-creator-name "Perhelion"
                        :source-creator-link "https://commons.wikimedia.org/wiki/User:Perhelion"})})

(def ^:private norman
  {:environment (let [width 5
                      height 8
                      half-width (/ width 2)
                      R (-> (/ 1 4)
                            (+ (* (/ height width)
                                  (/ height width)))
                            (* width))]
                  (environment/create
                   (str "m 0,0"
                        "h " half-width
                        "a " R " " R " 0 0 1 " (- half-width) " " height
                        "a " R " " R " 0 0 1 " (- half-width) " " (- height)
                        "z")
                   {:context :root
                    :bounding-box (bb/BoundingBox. (- half-width) half-width 0 height)
                    :points {:fess (v/Vector. 0 half-width)}}))
   :attribution heraldicon-attribution})

(def ^:private norman-rounded
  {:environment (let [width 5
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
                  (environment/create
                   (str "m 0,0"
                        "a " r " " r " 0 0 1 " half-width " " d
                        "a " R " " R " 0 0 1 " (- half-width) " " (- height d)
                        "a " R " " R " 0 0 1 " (- half-width) " " (- (- height d))
                        "a " r " " r " 0 0 1 " half-width " " (- d)
                        "z")
                   {:context :root
                    :bounding-box (bb/BoundingBox. (- half-width) half-width 0 height)
                    :points {:fess (v/Vector. 0 half-width)}}))
   :attribution heraldicon-attribution})

(def ^:private community-square-iberian-engrailed
  {:environment (environment/create
                 (str "m 0,0"
                      "a 250,250 0 0 0 250,0"
                      "a 250,250 0 0 0 250,0"
                      "v 350"
                      "a 250,250 0 0 1 -250,250"
                      "a 250,250 0 0 1 -250,-250"
                      "z")
                 {:context :root
                  :bounding-box (bb/BoundingBox. 0 500 0 600)})
   :attribution (merge heraldicon-attribution
                       {:nature :derivative
                        :source-license :public-domain
                        :source-link "https://heraldicon.org"
                        :source-name "Square Iberian Engrailed"
                        :source-creator-name "coinageFission#1205"
                        :source-creator-link "https://discord.com/channels/272117928298676225/272117928298676225"})})

(def ^:private community-kalasag
  {:environment (environment/create
                 "m 54.303084,263.03992 c 0.481806,0.77857 13.294025,21.48369
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
                 {:context :root
                  :bounding-box (bb/BoundingBox. 0 100 0 286.085)
                  :points {:fess (v/Vector. 50 143.0425)}})
   :attribution {:license :public-domain
                 :username "vairy"}})

(def ^:private community-louisiana
  {:environment (environment/create
                 "M 1.64826,87.810447 C 4.46558,85.281706 7.08513,82.261304
                 7.06447,76.558245 L 6.90748,33.595381 C 6.87368,24.346329
                 4.63582,22.134663 0,17.219029 4.3479,13.111909
                 7.94048,8.4189856 11.55175,3.4256966 21.25678,10.980354
                 40.09166,9.5735346 50,0 59.90834,9.5733441 78.74322,10.980164
                 88.44825,3.4256966 92.05961,8.4189856 95.65248,13.112
                 100,17.219029 c -4.63577,4.915634 -6.87367,7.1273
                 -6.90748,16.376352 l -0.15699,42.962864 c -0.0208,5.703059
                 2.5987,8.72332 5.41621,11.252202 C 87.51416,108.53152
                 61.422,95.64892 49.99796,110 38.5758,95.64892
                 12.48364,108.53128 1.64419,87.810447 Z"
                 {:context :root
                  :bounding-box (bb/BoundingBox. 0 100 0 110)
                  :points {:fess (v/Vector. 50 55)}})
   :attribution {:license :public-domain
                 :username "vairy"
                 :nature :derivative
                 :source-license :public-domain
                 :source-link "https://en.wikipedia.org/wiki/File:Louisiana_state_coat_of_arms_(illustrated,_1876).jpg"
                 :source-name "Louisiana state coat of arms (illustrated, 1876)"
                 :source-creator-name "Godot13"
                 :source-creator-link "https://commons.wikimedia.org/wiki/User:Godot13"}})

(def ^:private community-french-slim
  {:environment (environment/create
                 "m 0,0 c -0.051,33.464218 0.2004,86.58878 0.096,100
                 -0.1042,13.84023 8.933,24.42543 22.1005,24.82399
                 13.1676,0.39847 20.7982,1.73968 27.8035,13.02397 c
                 7.0052,-11.28472 14.6354,-12.62588 27.8034,-13.02397
                 13.168,-0.39808 22.2049,-10.98376 22.1006,-24.82399 C
                 99.8066,86.590696 100,33.464218 100,0 Z"
                 {:context :root
                  :bounding-box (bb/BoundingBox. 0 100 0 138.279)})
   :attribution {:license :public-domain
                 :username "vairy"}})

(def ^:private community-heater-bulgy
  {:environment (environment/create
                 "m 50,0 c 10,0 20,0 40,5 5,30 10,50 10,70 0,30 -25,45 -50,60 -25,-15 -50,-30 -50,-60
                 C -0.14921317,56.749847 5,35 10,5 c 20,-5 30,-5 40,-5 z"
                 {:context :root
                  :bounding-box (bb/BoundingBox. 0 100 0 135)})
   :attribution {:nature :own-work
                 :license :public-domain
                 :username "korfi2go"}})

(def ^:private community-heater-rounded
  {:environment (environment/create
                 "m 50,0 c 30,0 50,5 50,5 l 0.21875,34 C 100.21891,74.726721
                 80.940248,107.13664 50,125 19.059752,107.13664 -1.6239896e-4,74.726721
                 0,39 V 5 C 0,5 20,0 50,0 Z"
                 {:context :root
                  :bounding-box (bb/BoundingBox. 0 100 0 125)})
   :attribution {:nature :own-work
                 :license :public-domain
                 :username "korfi2go"}})

(def ^:private community-heater-pointed
  {:environment (environment/create
                 "m 50,0 c 30,5 50,5 50,5 l 0.21875,34 C 100.21891,74.726721 80.940248,107.13664
                 50,125 19.059752,107.13664 0,74.726721 0,39 V 5 C 0,5 20,5 50,0 Z"
                 {:context :root
                  :bounding-box (bb/BoundingBox. 0 100 0 125)})
   :attribution {:nature :own-work
                 :license :public-domain
                 :username "korfi2go"}})

(def ^:private community-angular
  {:environment (environment/create
                 "M 0,5 50,0 100,5 95,85 50,125 5,85 Z"
                 {:context :root
                  :bounding-box (bb/BoundingBox. 0 100 0 125)})
   :attribution {:nature :own-work
                 :license :public-domain
                 :username "korfi2go"}})

(def ^:private community-scutum
  {:environment (environment/create
                 "m 0,15 v 130 c 0,10 30,15 50,15 20,0 50,-5 50,-15
                 V 15 C 100,5 66.749793,0 50,0 30,0 0,5 0,15 Z"
                 {:context :root
                  :bounding-box (bb/BoundingBox. 0 100 0 160)
                  :points {:fess (v/Vector. 50 80)}})
   :attribution {:nature :own-work
                 :license :public-domain
                 :username "korfi2go"}})

(def ^:private community-american
  {:environment (environment/create
                 "M 15,0 0,15 c 0,0 10,5 10,15 0,15 -10,25 -10,50 0,15 5,30
                 25,35 15,3 25,10 25,10 0,0 10,-7 25,-10 20,-5 25,-20 25,-35 C
                 100,55 90,45 90,30 90,20 100,15 100,15 L 85,0 C 85,0 80,5 70,5
                 55,5 50,0 50,0 50,0 45,5 30,5 20,5 15,0 15,0 Z"
                 {:context :root
                  :bounding-box (bb/BoundingBox. 0 100 0 125)})
   :attribution {:nature :own-work
                 :license :public-domain
                 :username "korfi2go"}})

(def ^:private community-triangular
  {:environment (environment/create
                 "M 50,0 C 80,0 100,10 100,10 100,60 80,90 50,120 20,90
                 0,60.03607 0,10 0,10 20,0 50,0 Z"
                 {:context :root
                  :bounding-box (bb/BoundingBox. 0 100 0 120)})
   :attribution {:nature :own-work
                 :license :public-domain
                 :username "korfi2go"}})

(def ^:private community-german
  {:environment (environment/create
                 "m 0,20 c 7,-5 20,-5 20,7 0,13 -13,13 -20,8 0,0 7,9 5,15 -2,5
                 -5,15 -5,30 0,40 40,40 50,40 10,0 50,0 50,-40 C 100,65 97,55
                 95,50 91,40 90,35 90,30 90,28 90,8 100,0 70,0 55,5 40,10 30,5
                 25,4 15,5 0,7 0,17 0,20 Z"
                 {:context :root
                  :bounding-box (bb/BoundingBox. 0 100 0 125)})
   :attribution {:nature :own-work
                 :license :public-domain
                 :username "korfi2go"}})

(def ^:private community-african
  {:environment (environment/create
                 "M 45,0 C 45,0 0,15 0,80 c 0,65 45,80 45,80 0,0 45,-15 45,-80 C
                 90,15 45,0 45,0 Z"
                 {:context :root
                  :bounding-box (bb/BoundingBox. 0 90 0 160)})
   :attribution {:nature :own-work
                 :license :public-domain
                 :username "korfi2go"}})

(def ^:private community-italian
  {:environment (environment/create
                 "M 50,0 C 35,0 25,10 25,10 15,20 0,20 0,20 c 0,60 15,70 15,70 0,0 15,15 15,35 0,35 20,35 20,35 0,0 20,0 20,-35 0,-20 15,-35 15,-35 0,0 15,-10 15,-70 0,0 -15,0 -25,-10 C 75,10 65,0 50,0 Z"
                 {:context :root
                  :bounding-box (bb/BoundingBox. 0 100 0 160)})
   :attribution {:nature :own-work
                 :license :public-domain
                 :username "korfi2go"}})

(def ^:private community-nrw
  {:environment (environment/create
                 "M 10,7 C 5,7 0,5 0,5 0,5 2,10 2,15 v 65 c 0,15 13,25 23,30 20,10 25,15 25,15 0,0 5,-5 25,-15 10,-5 23,-15 23,-30 V 15 C 98,10 100,5 100,5 100,5 95,7 90,7 80,7 70,0 60,0 50,0 50,5 50,5 50,5 50,0 40,0 30,0 20,7 10,7 Z"
                 {:context :root
                  :bounding-box (bb/BoundingBox. 0 100 0 125)})
   :attribution {:nature :own-work
                 :license :public-domain
                 :username "korfi2go"}})

(def ^:private community-rhombus
  {:environment (environment/create
                 "M 50,0 L 100,50 L 50,100 L 0,50 Z"
                 {:context :root
                  :bounding-box (bb/BoundingBox. 0 100 0 100)})
   :attribution {:nature :own-work
                 :license :public-domain
                 :username "Bananasplit1611"}})

(def ^:private community-queens-consort
  {:environment (environment/create
                 "m 50,0 c 0,0 -9.494151,12.643461 -22.473589,10.987261 0,0 -4.767819,10.401769 -27.526411,6.654218 0,0 14.196854,35.081974 3.7934578,71.435548 0,0 16.0784912,1.395807 23.6211622,16.054983 0,0 12.815277,-3.15411 22.58538,4.86799 9.770103,-8.0221 22.58538,-4.86799 22.58538,-4.86799 C 80.12805,90.472834 96.206541,89.077027 96.206541,89.077027 85.803143,52.723453 100,17.641479 100,17.641479 77.24141,21.38903 72.473589,10.987261 72.473589,10.987261 59.494151,12.643461 50,0 50,0 Z"
                 {:context :root
                  :bounding-box (bb/BoundingBox. 0 100 0 110)})
   :attribution {:nature :own-work
                 :license :public-domain
                 :username "Azure"}})

(def ^:private escutcheons
  [[:string.escutcheon.group/traditional
    [:string.escutcheon.type/heater #'heater]
    [:string.escutcheon.type/square-french #'square-french]
    [:string.escutcheon.type/square-iberian #'square-iberian]
    [:string.escutcheon.type/square-czech #'square-czech]
    [:string.escutcheon.type/french-modern #'french-modern]
    [:string.escutcheon.type/english #'english]
    [:string.escutcheon.type/kite #'kite]
    [:string.escutcheon.type/rounded-norman-late #'norman-rounded]
    [:string.escutcheon.type/norman-late #'norman]]
   [:string.escutcheon.group/shapes
    [:string.escutcheon.type/flag #'flag]
    [:string.escutcheon.type/rectangle #'rectangle]
    [:string.escutcheon.type/lozenge #'lozenge]
    [:string.escutcheon.type/roundel #'roundel]
    [:string.escutcheon.type/oval #'oval]]
   [:string.escutcheon.group/decorative
    [:string.escutcheon.type/wedge #'wedge]
    [:string.escutcheon.type/swiss #'swiss]
    [:string.escutcheon.type/renaissance #'renaissance]
    [:string.escutcheon.type/polish #'polish]
    [:string.escutcheon.type/polish-19th-century #'polish-19th-century]]
   [:string.escutcheon.group/community
    ["Square Iberian Engrailed" #'community-square-iberian-engrailed]
    ["Angular" #'community-angular]
    ["Bulgy Heater" #'community-heater-bulgy]
    ["Pointed Heater" #'community-heater-pointed]
    ["Rounded Heater" #'community-heater-rounded]
    ["Triangular" #'community-triangular]
    ["American" #'community-american]
    ["Italian" #'community-italian]
    ["German" #'community-german]
    ["NRW" #'community-nrw]
    ["French Slim" #'community-french-slim]
    ["Louisiana" #'community-louisiana]
    ["Kalasag" #'community-kalasag]
    ["African" #'community-african]
    ["Scutum" #'community-scutum]
    ["Rhombus" #'community-rhombus]
    ["Queen's Consort" #'community-queens-consort]]])

(def ^:private kinds-map
  (into {}
        (for [[_ & items] escutcheons
              [_ v] items]
          [(-> v meta :name keyword) @v])))

(def choices
  (into [[:string.escutcheon.type/none
          [:string.escutcheon.type/none :none]]]
        (map (fn [[group-name & items]]
               (into [group-name]
                     (map (fn [[display-name v]]
                            [display-name (-> v meta :name keyword)]))
                     items)))
        escutcheons))

(def escutcheon-map
  (options/choices->map choices))

(defn attribution [escutcheon-type]
  (:attribution (get kinds-map escutcheon-type)))

(defn field [escutcheon-type flag-width flag-height swallow-tail tail-point-height tail-tongue]
  (let [escutcheon-data (get kinds-map escutcheon-type)]
    (if (= escutcheon-type :flag)
      ((:function escutcheon-data) flag-width flag-height swallow-tail tail-point-height tail-tongue)
      (:environment escutcheon-data))))

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
