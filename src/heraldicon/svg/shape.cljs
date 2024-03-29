(ns heraldicon.svg.shape
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.svg.infinity :as infinity]
   [heraldicon.svg.path :as path]))

(defn- inspect-part [part]
  (cond
    (map? part) {:line part}
    (and (vector? part)
         (= (first part) :reverse)) {:line (second part)
                                     :reverse? true}
    (= part
       :full) {:infinity infinity/full}
    (= part
       :clockwise) {:infinity infinity/clockwise}
    (= part
       :clockwise-shortest) {:infinity infinity/clockwise
                             :shortest? true}
    (= part
       :counter-clockwise) {:infinity infinity/counter-clockwise}
    (= part
       :counter-clockwise-shortest) {:infinity infinity/counter-clockwise
                                     :shortest? true}))

(defn- close-path [path]
  (conj path "z"))

(defn- add-part [center
                 {:keys [path current]}
                 {:keys [line reverse? infinity shortest?]}
                 {next-line :line
                  next-reverse? :reverse?}]
  (cond
    line {:path (conj path
                      (if (empty? path)
                        "M" "L")
                      (line/line-start line :reverse? reverse?)
                      (path/stitch (cond-> (:line line)
                                     reverse? line/reversed-path)))
          :current (line/line-end line :reverse? reverse?)}
    infinity (let [next-point (line/line-start next-line :reverse? next-reverse?)]
               {:path (conj path (infinity center current next-point :shortest? shortest?))
                :current next-point})))

(defn- subfield-context? [context]
  (and (-> context :path last integer?)
       (-> context :path drop-last last (= :fields))))

(defn- transform-fn [context]
  (when (subfield-context? context)
    (:transform-fn (interface/get-properties (c/-- context 2)))))

(defn build-shape [context & parts]
  (let [transform-fn* (or (transform-fn context) identity)
        center (transform-fn* (bb/center (interface/get-bounding-box context)))]
    (->> (partition 2 1 parts parts)
         (reduce (fn [state [part next-part]]
                   (let [part (inspect-part part)
                         next-part (inspect-part next-part)]
                     (add-part center state part next-part)))
                 {:path []
                  :current nil})
         :path
         close-path
         path/make-path)))
