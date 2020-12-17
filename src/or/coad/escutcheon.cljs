(ns or.coad.escutcheon)

(defn average [[x1 y1] [x2 y2]]
  [(/ (+ x1 x2) 2)
   (/ (+ y1 y2) 2)])

(defn calculate [data]
  (let [chief        (get-in data [:points :chief])
        base         (get-in data [:points :base])
        dexter       (get-in data [:points :dexter])
        sinister     (get-in data [:points :sinister])
        middle-chief (get-in data [:points :middle-chief])
        fess         (get-in data [:points :fess])
        honour       (average middle-chief fess)
        nombril      (average honour base)
        min-x        (apply min (map first [chief base dexter sinister]))
        max-x        (apply max (map first [chief base dexter sinister]))
        min-y        (apply min (map second [chief base dexter sinister]))
        max-y        (apply max (map second [chief base dexter sinister]))
        width        (- max-x min-x)
        height       (- max-y min-y)
        scale        (/ 100 width)
        transform    (str "scale(" scale "," scale ") translate(" (- min-x) "," (- min-y) ")")]
    (-> data
        (assoc-in [:points :nombril] nombril)
        (assoc-in [:points :honour] honour)
        (assoc-in [:points :origin] [min-x min-y])
        (assoc-in [:width] width)
        (assoc-in [:height] height)
        (assoc-in [:scale] scale)
        (assoc-in [:transform] transform))))

(def heater
  ;; sqrt(3) / 2 * 6 ~ 5.196152422706632
  (calculate
   {:name   "Heater"
    :shape  (str "m 0,0"
                 "h 3"
                 "v 2"
                 "a 6 6 0 0 1 -3,5.196152422706632"
                 "a 6 6 0 0 1 -3,-5.196152422706632"
                 "v -2"
                 "z")
    :points {:chief          [0 0]
             :dexter-chief   [-2 1]
             :middle-chief   [0 1]
             :sinister-chief [2 1]
             :dexter         [-3 3]
             :sinister       [3 3]
             :base           [0 7.196152422706632]
             :middle-base    [0 6.196152422706632]
             :fess           [0 3]}}))

(def square-iberian
  (calculate
   {:name   "Square Iberian"
    :shape  (str "m 0,0"
                 "h 5"
                 "v 7"
                 "a 5 5 0 0 1 -10,0"
                 "v -7"
                 "z")
    :points {:chief          [0 0]
             :dexter-chief   [-5 1]
             :middle-chief   [0 1]
             :sinister-chief [5 1]
             :dexter         [-5 4]
             :sinister       [5 4]
             :base           [0 12]
             :middle-base    [0 11]
             :fess           [0 5]}}))
