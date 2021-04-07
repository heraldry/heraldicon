(ns heraldry.coat-of-arms.escutcheon
  (:require [heraldry.coat-of-arms.field.environment :as environment]))

(def
  ^{:display-name "Heater"}
  heater
  ;; sqrt(3) / 2 * 6 ~ 5.196152422706632
  (environment/create
   (str "m 0,0"
        "h 3"
        "v 2"
        "a 6 6 0 0 1 -3,5.196152422706632"
        "a 6 6 0 0 1 -3,-5.196152422706632"
        "v -2"
        "z")
   {:context      :root
    :bounding-box [-3 3 0 (+ 2 5.196152422706632)]}))

(def
  ^{:display-name "Square French"}
  square-french
  (environment/create
   (str "m 0,0"
        "v 15.7"
        "c 0,6 6,12 12,13"
        "c 6,-1 12,-7 12,-13"
        "V 0"
        "z")
   {:context      :root
    :bounding-box [0 (* 2 12) 0 (+ 15.7 13)]}))

(def
  ^{:display-name "Square Iberian"}
  square-iberian
  (environment/create
   (str "m 0,0"
        "h 5"
        "v 7"
        "a 5 5 0 0 1 -10,0"
        "v -7"
        "z")
   {:context      :root
    :bounding-box [-5 5 0 (+ 7 5)]}))

(def
  ^{:display-name "Square Czech"}
  square-czech
  (environment/create
   ;; diff(sqrt(r*r - x*x))
   ;; solve(-24/sqrt(r^2 - 24^2) - (-35/4)) ~ 24.156226
   (str "m 0,0"
        "h 56"
        "l -4,35"
        "a 24.156226 24.156226 0 0 1 -48,0"
        "z")
   {:context      :root
    :bounding-box [0 56 0 56.5]}))

(def
  ^{:display-name "French modern"}
  french-modern
  (environment/create
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
   {:context      :root
    :bounding-box [-7 7 0 (* 2 8)]}))

(def
  ^{:display-name "Lozenge"}
  lozenge
  (environment/create
   (str "m 0,0"
        "L 5,6.5"
        "L 0,13"
        "L -5,6.5"
        "z")
   {:context      :root
    :bounding-box [-5 5 0 13]
    :points       {:fess {:x 0 :y 6.5}}}))

(def
  ^{:display-name "Oval"}
  oval
  (environment/create
   (str "m 0,0"
        "A 5 6.8 0 0 1 5,6.5"
        "A 5 6.8 0 0 1 0,13"
        "A 5 6.8 0 0 1 -5,6.5"
        "A 5 6.8 0 0 1 0,0"
        "z")
   {:context      :root
    :bounding-box [-5 5 0 13]
    :points       {:fess {:x 0 :y 6.5}}}))

(def
  ^{:display-name "Roundel"}
  roundel
  (environment/create
   (str "m 0,0"
        "a 5 5 0 0 1 0,10"
        "a 5 5 0 0 1 0,-10"
        "z")
   {:context      :root
    :bounding-box [-5 5 0 10]
    :points       {:fess {:x 0 :y 5}}}))

(def
  ^{:display-name "Swiss"}
  swiss
  ;; sqrt(3) / 2 * 6 ~ 5.196152422706632
  (environment/create
   (str "m 0,0"
        "a 6 6 0 0 0 3,0"
        "v 2"
        "a 6 6 0 0 1 -3,5.196152422706632"
        "a 6 6 0 0 1 -3,-5.196152422706632"
        "v -2"
        "a 6 6 0 0 0 3,0"
        "z")
   {:context      :root
    :bounding-box [-3 3 0 (+ 2 5.196152422706632)]}))

(def
  ^{:display-name "English"}
  english
  (environment/create
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
   {:context      :root
    :bounding-box [-8 8 0 (+ (* 2 8) 1)]}))

(def
  ^{:display-name "Polish"}
  polish
  (environment/create
   (str "m 43.402145,5e-7 "
        "c -8.662508,0 -14.063932,7.322064 -27.53457,9.380727 0.01086,7.9371285 -3.321499,15.7448405 -7.7644202,20.8881635 0,0 8.6550412,4.035941 8.6550412,12.967045 0,13.48538 -14.3402146,13.50873 -14.3402146,13.50873 0,0 -2.4179809,4.962539 -2.4179809,15.009696 0,22.996861 15.7236635,40.377428 27.6621895,45.737558 11.938525,5.36013 18.80961,7.63894 22.359194,12.50808 3.549585,-4.86914 10.377904,-7.14795 22.316426,-12.50808 11.938526,-5.36013 27.662185,-22.742701 27.662185,-45.737557 0,-10.047158 -2.41798,-15.009697 -2.41798,-15.009697 0,0 -14.340209,-0.02335 -14.340209,-13.50873 0,-8.931104 8.655042,-12.967045 8.655042,-12.967045 "
        "C 87.453242,25.123567 84.122242,17.317856 84.132428,9.3807275 70.661111,7.3213975 65.259687,5.0000001e-7 56.597858,5.0000001e-7 51.658715,5.0000001e-7 50.021384,2.5016165 50.021384,2.5016165 "
        "c 0,0 -1.680096,-2.50161599999999 -6.619239,-2.501616 "
        "z")
   {:context      :root
    :bounding-box [0 100 0 130]
    :points       {:fess {:x 50 :y 60}}}))

(def
  ^{:display-name "Polish 19th century"}
  polish-19th-century
  (environment/create
   (str
    "M 9.5919374,7.6420451e-7 6.7196191e-7,9.9320533 "
    "C 13.91585,26.565128 6.4383768,51.856026 6.0545095,76.190405 5.7210271,97.330758 24.557556,120 50.136084,120 75.714614,120 94.551144,97.330758 94.217662,76.190405 93.833795,51.856026 86.356321,26.565129 100.27217,9.9320533 "
    "L 90.680234,7.6420451e-7 "
    "C 81.317854,12.169833 65.149597,3.8094085 50.136084,3.8094085 35.122571,3.8094085 18.954318,12.169833 9.5919374,7.6420451e-7 "
    "Z")
   {:context      :root
    :bounding-box [0 100 0 120]
    :points       {:fess {:x 50 :y 60}}}))

(def
  ^{:display-name "Renaissance"}
  renaissance
  (environment/create
   (str
    "M 43.672061,112.35743 "
    "C 20.076921,107.21428 1.2267205,96.616647 5.1084778e-7,62.761658 9.9757105,57.299078 13.336031,28.673358 3.0804505,13.816518 "
    "L 9.0622405,3.6100493 "
    "C 28.967341,6.8985193 35.708501,-4.5443607 50,2.1304593 "
    "c 14.2915,-6.67482 21.03266,4.76806 40.93775,1.47959 "
    "l 5.9818,10.2064687 "
    "C 86.66397,28.673358 90.02428,57.299078 100,62.761658 98.77327,96.616647 79.92307,107.21428 56.32792,112.35743 51.60688,113.38653 51.68278,114.71878 50,117 "
    "c -1.68279,-2.28122 -1.60689,-3.61347 -6.327939,-4.64257 "
    "Z")
   {:context      :root
    :bounding-box [0 100 0 117]
    :points       {:fess {:x 50 :y 55}}}))

(def
  ^{:display-name "Rectangle"}
  rectangle
  (environment/create
   (str
    "M 0,0"
    "h 10"
    "v 12"
    "h -10"
    "z")
   {:context      :root
    :bounding-box [0 10 0 12]
    :points       {:fess {:x 5 :y 6}}}))

(def
  ^{:display-name "Flag"}
  flag
  (environment/create
   (str
    "M 0,0"
    "h 15"
    "v 10"
    "h -15"
    "z")
   {:context      :root
    :bounding-box [0 15 0 10]
    :points       {:fess {:x 7.5 :y 5}}}))

(def escutcheons
  [#'heater
   #'square-french
   #'square-iberian
   #'square-czech
   #'french-modern
   #'lozenge
   #'roundel
   #'oval
   #'renaissance
   #'swiss
   #'english
   #'polish
   #'polish-19th-century
   #'rectangle
   #'flag])

(def kinds-map
  (->> escutcheons
       (map (fn [v]
              [(-> v meta :name keyword) (deref v)]))
       (into {})))

(def choices
  (->> escutcheons
       (map (fn [v]
              [(-> v meta :display-name) (-> v meta :name keyword)]))))

(defn field [type]
  (get kinds-map type))

