(ns heraldry.coat-of-arms.cottising
  (:require
   [heraldry.coat-of-arms.default :as default]
   [heraldry.coat-of-arms.field.options :as field-options]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.math.vector :as v]
   [heraldry.strings :as strings]))

(def cottise-default-options
  {:line (-> line/default-options
             (assoc-in [:ui :label] strings/line))
   :opposite-line (-> line/default-options
                      (assoc-in [:ui :label] strings/opposite-line))
   :distance {:type :range
              :min -10
              :max 20
              :default 2
              :ui {:label {:en "Distance"
                           :de "Abstand"}
                   :step 0.1}}
   :thickness {:type :range
               :min 0.1
               :max 20
               :default 2
               :ui {:label strings/thickness
                    :step 0.1}}
   :field (field-options/options default/field)
   :outline? {:type :boolean
              :default false
              :ui {:label strings/outline}}
   :ui {:form-type :cottising}})

(def default-options
  {:cottise-1 cottise-default-options
   :cottise-2 cottise-default-options
   :cottise-opposite-1 cottise-default-options
   :cottise-opposite-2 cottise-default-options
   :cottise-extra-1 cottise-default-options
   :cottise-extra-2 cottise-default-options})

(defn cottise-options [options {:keys [line opposite-line field]}]
  (cond-> options
    (:line options)
    (assoc :line (-> (line/options line)
                     (assoc :ui (-> cottise-default-options :line :ui))))

    (:opposite-line options)
    (assoc :opposite-line (-> (line/options opposite-line)
                              (assoc :ui (-> cottise-default-options :opposite-line :ui))))

    (:field options)
    (assoc :field (field-options/options field))))

(defn options [options {:keys [cottise-1 cottise-2
                               cottise-opposite-1 cottise-opposite-2
                               cottise-extra-1 cottise-extra-2]}]
  (cond-> options
    (:cottise-1 options)
    (update :cottise-1 cottise-options cottise-1)

    (:cottise-2 options)
    (update :cottise-2 cottise-options cottise-2)

    (:cottise-opposite-1 options)
    (update :cottise-opposite-1 cottise-options cottise-opposite-1)

    (:cottise-opposite-2 options)
    (update :cottise-opposite-2 cottise-options cottise-opposite-2)

    (:cottise-extra-1 options)
    (update :cottise-extra-1 cottise-options cottise-extra-1)

    (:cottise-extra-2 options)
    (update :cottise-extra-2 cottise-options cottise-extra-2)))

(defn render-fess-cottise [{:keys [environment] :as context}
                           cottise-2-key next-cottise-key
                           & {:keys [offset-y-fn
                                     alignment
                                     swap-lines?]}]
  (when (interface/get-raw-data context)
    (let [line (interface/get-sanitized-data (c/++ context :line))
          opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
          [line opposite-line] (if swap-lines?
                                 [opposite-line line]
                                 [line opposite-line])
          thickness (interface/get-sanitized-data (c/++ context :thickness))
          distance (interface/get-sanitized-data (c/++ context :distance))
          outline? (interface/get-sanitized-data (c/++ context :outline?))]
      [ordinary-interface/render-ordinary
       (-> context
           (assoc :path [:context :cottise])
           (assoc :cottise {:type :heraldry.ordinary.type/fess
                            :field (interface/get-raw-data (c/++ context :field))
                            :line line
                            :opposite-line opposite-line
                            :geometry {:size thickness}
                            :cottising {next-cottise-key (interface/get-raw-data
                                                          (update context :path
                                                                  (fn [path]
                                                                    (-> path
                                                                        drop-last
                                                                        vec
                                                                        (conj cottise-2-key)))))}
                            :origin {:point :fess
                                     :offset-y [:force (offset-y-fn
                                                        (-> environment :points :fess :y)
                                                        distance)]
                                     :alignment alignment}
                            :outline? outline?}))])))

(defn render-pale-cottise [{:keys [environment] :as context}
                           cottise-2-key next-cottise-key
                           & {:keys [offset-x-fn
                                     alignment
                                     swap-lines?]}]
  (when (interface/get-raw-data context)
    (let [line (interface/get-sanitized-data (c/++ context :line))
          opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
          [line opposite-line] (if swap-lines?
                                 [opposite-line line]
                                 [line opposite-line])
          thickness (interface/get-sanitized-data (c/++ context :thickness))
          distance (interface/get-sanitized-data (c/++ context :distance))
          outline? (interface/get-sanitized-data (c/++ context :outline?))]
      [ordinary-interface/render-ordinary
       (-> context
           (assoc :path [:context :cottise])
           (assoc :cottise {:type :heraldry.ordinary.type/pale
                            :field (interface/get-raw-data (c/++ context :field))
                            :line line
                            :opposite-line opposite-line
                            :geometry {:size thickness}
                            :cottising {next-cottise-key (interface/get-raw-data
                                                          (update context :path
                                                                  (fn [path]
                                                                    (-> path
                                                                        drop-last
                                                                        vec
                                                                        (conj cottise-2-key)))))}
                            :origin {:point :fess
                                     :offset-x [:force (offset-x-fn
                                                        (-> environment :points :fess :y)
                                                        distance)]
                                     :alignment alignment}
                            :outline? outline?}))])))

(defn render-bend-cottise [{:keys [environment] :as context}
                           cottise-2-key next-cottise-key
                           & {:keys [distance-fn
                                     alignment
                                     swap-lines?
                                     angle
                                     direction-orthogonal
                                     center-point
                                     width
                                     height
                                     middle-real-start-fn
                                     middle-real-end-fn
                                     sinister?]}]
  (when (interface/get-raw-data context)
    (let [line (interface/get-sanitized-data (c/++ context :line))
          opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
          [line opposite-line] (if swap-lines?
                                 [opposite-line line]
                                 [line opposite-line])
          thickness (interface/get-sanitized-data (c/++ context :thickness))
          distance (interface/get-sanitized-data (c/++ context :distance))
          effective-distance (distance-fn distance thickness)
          point-offset (v/mul direction-orthogonal effective-distance)
          new-center-point (v/add center-point point-offset)
          fess-offset (v/sub new-center-point (-> environment :points :fess))
          outline? (interface/get-sanitized-data (c/++ context :outline?))]
      [ordinary-interface/render-ordinary
       (-> context
           (assoc :path [:context :cottise])
           (assoc :cottise {:type (if sinister?
                                    :heraldry.ordinary.type/bend-sinister
                                    :heraldry.ordinary.type/bend)
                            :field (interface/get-raw-data (c/++ context :field))
                            :line line
                            :opposite-line opposite-line
                            :geometry {:size thickness}
                            :cottising {next-cottise-key (interface/get-raw-data
                                                          (update context :path
                                                                  (fn [path]
                                                                    (-> path
                                                                        drop-last
                                                                        vec
                                                                        (conj cottise-2-key)))))}
                            :origin {:point :fess
                                     :offset-x [:force (-> fess-offset
                                                           :x
                                                           (/ width)
                                                           (* 100))]
                                     :offset-y [:force (-> fess-offset
                                                           :y
                                                           (/ height)
                                                           (* 100)
                                                           -)]
                                     :alignment alignment}
                            :anchor {:point :angle
                                     :angle angle}
                            :outline? outline?})
           (assoc :override-center-point new-center-point)
           (assoc :override-middle-real-start (middle-real-start-fn point-offset))
           (assoc :override-middle-real-end (middle-real-end-fn point-offset)))])))

(defn render-chevron-cottise [{:keys [environment] :as context}
                              cottise-2-key next-cottise-key
                              & {:keys [distance-fn
                                        alignment
                                        swap-lines?
                                        width
                                        height
                                        joint-angle
                                        chevron-angle
                                        corner-point]}]
  (when (interface/get-raw-data context)
    (let [line (interface/get-sanitized-data (c/++ context :line))
          opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
          [line opposite-line] (if swap-lines?
                                 [opposite-line line]
                                 [line opposite-line])
          thickness (interface/get-sanitized-data (c/++ context :thickness))
          distance (interface/get-sanitized-data (c/++ context :distance))
          half-joint-angle (/ joint-angle 2)
          half-joint-angle-rad (-> half-joint-angle
                                   (/ 180)
                                   (* Math/PI))
          effective-distance (distance-fn distance half-joint-angle-rad)
          point-offset (-> (v/v effective-distance 0)
                           (v/rotate chevron-angle)
                           (v/add corner-point))
          fess-offset (v/sub point-offset (-> environment :points :fess))
          outline? (interface/get-sanitized-data (c/++ context :outline?))]
      [ordinary-interface/render-ordinary
       (-> context
           (assoc :path [:context :cottise])
           (assoc :cottise {:type :heraldry.ordinary.type/chevron
                            :field (interface/get-raw-data (c/++ context :field))
                            :line line
                            :opposite-line opposite-line
                            :geometry {:size thickness}
                            :cottising {next-cottise-key (interface/get-raw-data
                                                          (update context :path
                                                                  (fn [path]
                                                                    (-> path
                                                                        drop-last
                                                                        vec
                                                                        (conj cottise-2-key)))))}
                            :origin {:point :fess
                                     :offset-x [:force (-> fess-offset
                                                           :x
                                                           (/ width)
                                                           (* 100))]
                                     :offset-y [:force (-> fess-offset
                                                           :y
                                                           (/ height)
                                                           (* 100)
                                                           -)]
                                     :alignment alignment}
                            :anchor {:point :angle
                                     :angle [:force half-joint-angle]}
                            :direction-anchor {:point :angle
                                               :angle [:force (- chevron-angle 90)]}
                            :outline? outline?}))])))

(defmethod interface/component-options :heraldry.component/cottise [_path _data]
  nil)
