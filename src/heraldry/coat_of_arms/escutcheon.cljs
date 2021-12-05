(ns heraldry.coat-of-arms.escutcheon
  (:require
   [heraldry.coat-of-arms.field.environment :as environment]
   [heraldry.gettext :refer [string]]
   [heraldry.util :as util]))

(def heater
  {:display-name (string "Heater")
   ;; sqrt(3) / 2 * 6 ~ 5.196152422706632
   :environment (environment/create
                 (str "m 0,0"
                      "h 3"
                      "v 2"
                      "a 6 6 0 0 1 -3,5.196152422706632"
                      "a 6 6 0 0 1 -3,-5.196152422706632"
                      "v -2"
                      "z")
                 {:context :root
                  :bounding-box [-3 3 0 (+ 2 5.196152422706632)]})})

(def square-french
  {:display-name (string "Square French")
   :environment (environment/create
                 (str "m 0,0"
                      "v 15.7"
                      "c 0,6 6,12 12,13"
                      "c 6,-1 12,-7 12,-13"
                      "V 0"
                      "z")
                 {:context :root
                  :bounding-box [0 (* 2 12) 0 (+ 15.7 13)]})})

(def square-iberian
  {:display-name (string "Square Iberian")
   :environment (environment/create
                 (str "m 0,0"
                      "h 5"
                      "v 7"
                      "a 5 5 0 0 1 -10,0"
                      "v -7"
                      "z")
                 {:context :root
                  :bounding-box [-5 5 0 (+ 7 5)]})})

(def square-czech
  {:display-name (string "Square Czech")
   :environment (environment/create
                 ;; diff(sqrt(r*r - x*x))
                 ;; solve(-24/sqrt(r^2 - 24^2) - (-35/4)) ~ 24.156226
                 (str "m 0,0"
                      "h 56"
                      "l -4,35"
                      "a 24.156226 24.156226 0 0 1 -48,0"
                      "z")
                 {:context :root
                  :bounding-box [0 56 0 56.5]})})

(def french-modern
  {:display-name (string "French modern")
   :environment (environment/create
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
                  :bounding-box [-7 7 0 (* 2 8)]})})

(def lozenge
  {:display-name (string "Lozenge [escutcheon]")
   :environment (environment/create
                 (str "m 0,0"
                      "L 5,6.5"
                      "L 0,13"
                      "L -5,6.5"
                      "z")
                 {:context :root
                  :bounding-box [-5 5 0 13]
                  :points {:fess {:x 0 :y 6.5}}})})

(def oval
  {:display-name (string "Oval")
   :environment (environment/create
                 (str "m 0,0"
                      "A 5 6.8 0 0 1 5,6.5"
                      "A 5 6.8 0 0 1 0,13"
                      "A 5 6.8 0 0 1 -5,6.5"
                      "A 5 6.8 0 0 1 0,0"
                      "z")
                 {:context :root
                  :bounding-box [-5 5 0 13]
                  :points {:fess {:x 0 :y 6.5}}})})

(def roundel
  {:display-name (string "Roundel [escutcheon]")
   :environment (environment/create
                 (str "m 0,0"
                      "a 5 5 0 0 1 0,10"
                      "a 5 5 0 0 1 0,-10"
                      "z")
                 {:context :root
                  :bounding-box [-5 5 0 10]
                  :points {:fess {:x 0 :y 5}}})})

(def swiss
  {:display-name (string "Swiss [escutcheon]")
   ;; sqrt(3) / 2 * 6 ~ 5.196152422706632
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
                  :bounding-box [-3 3 0 (+ 2 5.196152422706632)]})})

(def english
  {:display-name (string "English [escutcheon]")
   :environment (environment/create
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
                  :bounding-box [-8 8 0 (+ (* 2 8) 1)]})})

(def polish
  {:display-name (string "Polish [escutcheon]")
   :environment (environment/create
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
                  :bounding-box [0 100 0 130]
                  :points {:fess {:x 50 :y 60}}})})

(def polish-19th-century
  {:display-name (string "Polish 19th century")
   :environment (environment/create
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
                  :bounding-box [0 100 0 120]
                  :points {:fess {:x 50 :y 60}}})})

(def renaissance
  {:display-name (string "Renaissance [escutcheon]")
   :environment (environment/create
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
                  :bounding-box [0 100 0 117]
                  :points {:fess {:x 50 :y 55}}})})

(def rectangle
  {:display-name (string "Rectangle [escutcheon]")
   :environment (environment/create
                 (str
                  "M 0,0"
                  "h 10"
                  "v 12"
                  "h -10"
                  "z")
                 {:context :root
                  :bounding-box [0 10 0 12]
                  :points {:fess {:x 5 :y 6}}})})

(def flag
  {:display-name (string "Flag")
   :function (fn [width height swallow-tail tail-point-height tail-tongue]
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
                   :bounding-box [0 width 0 height]
                   :points {:fess {:x (/ width 2) :y (/ height 2)}}})))
   :environment (environment/create
                 (str
                  "M 0,0"
                  "h 5"
                  "v 3"
                  "h -5"
                  "z")
                 {:context :root
                  :bounding-box [0 5 0 3]
                  :points {:fess {:x 2.5 :y 1.5}}})})

(def wedge
  {:display-name (string "Wedge [escutcheon]")
   ;; sqrt(3) / 2 * 6 + 2 ~ 7.196152422706632
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
                    :bounding-box [-3 3 0 7.196152422706632]}))})

(def kite
  {:display-name (string "Kite")
   :environment (let [width 1
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
                    :bounding-box [(- half-width) half-width 0 height]
                    :points {:fess {:x 0 :y half-width}}}))})

(def norman
  {:display-name (string "Norman (late)")
   :environment (let [width 5
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
                    :bounding-box [(- half-width) half-width 0 height]
                    :points {:fess {:x 0 :y half-width}}}))})

(def norman-rounded
  {:display-name (string "Rounded Norman (late)")
   :environment (let [width 5
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
                    :bounding-box [(- half-width) half-width 0 height]
                    :points {:fess {:x 0 :y half-width}}}))})

(def escutcheons
  [#'heater
   #'square-french
   #'square-iberian
   #'square-czech
   #'wedge
   #'swiss
   #'renaissance
   #'polish
   #'polish-19th-century
   #'lozenge
   #'roundel
   #'oval
   #'kite
   #'norman-rounded
   #'norman
   #'french-modern
   #'english
   #'flag
   #'rectangle])

(def kinds-map
  (->> escutcheons
       (map (fn [v]
              [(-> v meta :name keyword) (deref v)]))
       (into {})))

(def choices
  (->> escutcheons
       (map (fn [v]
              [(-> v deref :display-name) (-> v meta :name keyword)]))
       (into [[(string "None") :none]])))

(def choice-map
  (util/choices->map choices))

(defn field [escutcheon-type flag-width flag-height swallow-tail tail-point-height tail-tongue]
  (let [escutcheon-data (get kinds-map escutcheon-type)]
    (if (= escutcheon-type :flag)
      ((:function escutcheon-data) flag-width flag-height swallow-tail tail-point-height tail-tongue)
      (:environment escutcheon-data))))

(def flag-options
  {:flag-aspect-ratio-preset {:type :choice
                              :choices [[(util/str-tr "--- " (string "Select ratio") " ---") :none]
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
                              :ui {:label (string "Aspect ratio preset")
                                   :form-type :flag-aspect-ratio-preset-select}}

   :flag-width {:type :range
                :default 3
                :min 0.1
                :max 41
                :ui {:label (string "Flag width")
                     :step 0.01}}

   :flag-height {:type :range
                 :default 2
                 :min 0.1
                 :max 41
                 :ui {:label (string "Flag height")
                      :step 0.01}}

   :flag-swallow-tail {:type :range
                       :default 0
                       :min 0
                       :max 100
                       :ui {:label (string "Flag swallow tail")
                            :step 0.01}}

   :flag-tail-point-height {:type :range
                            :default 0
                            :min 0
                            :max 90
                            :ui {:label (string "Flag tail point height")
                                 :step 0.01}}

   :flag-tail-tongue {:type :range
                      :default 0
                      :min 0
                      :max 100
                      :ui {:label (string "Flag tongue")
                           :step 0.01}}})
