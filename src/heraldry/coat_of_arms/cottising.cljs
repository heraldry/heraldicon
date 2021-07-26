(ns heraldry.coat-of-arms.cottising
  (:require [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.field.options :as field-options]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.interface :as interface]))

(def cottise-default-options
  {:line (-> line/default-options
             (assoc-in [:ui :label] "Line"))
   :opposite-line (-> line/default-options
                      (assoc-in [:ui :label] "Opposite line"))
   :distance {:type :range
              :min -10
              :max 20
              :default 2
              :ui {:label "Distance"
                   :step 0.1}}
   :thickness {:type :range
               :min 0.1
               :max 20
               :default 2
               :ui {:label "Thickness"
                    :step 0.1}}
   :field (field-options/options default/field)
   :outline? {:type :boolean
              :default false
              :ui {:label "Outline"}}
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
    (and (:line options)
         line)
    (assoc :line (-> (line/options line)
                     (assoc :ui (-> cottise-default-options :line :ui))))

    (and (:opposite-line options)
         opposite-line)
    (assoc :opposite-line (-> (line/options opposite-line)
                              (assoc :ui (-> cottise-default-options :opposite-line :ui))))

    (and (:field options)
         field)
    (assoc :field (field-options/options field))))

(defn options [options {:keys [cottise-1 cottise-2
                               cottise-opposite-1 cottise-opposite-2
                               cottise-extra-1 cottise-extra-2]}]
  (cond-> options
    (and (:cottise-1 options)
         cottise-1)
    (update :cottise-1 cottise-options cottise-1)

    (and (:cottise-2 options)
         cottise-2)
    (update :cottise-2 cottise-options cottise-2)

    (and (:cottise-opposite-1 options)
         cottise-opposite-1)
    (update :cottise-opposite-1 cottise-options cottise-opposite-1)

    (and (:cottise-opposite-2 options)
         cottise-opposite-2)
    (update :cottise-opposite-2 cottise-options cottise-opposite-2)

    (and (:cottise-extra-1 options)
         cottise-extra-1)
    (update :cottise-extra-1 cottise-options cottise-extra-1)

    (and (:cottise-extra-2 options)
         cottise-extra-2)
    (update :cottise-extra-2 cottise-options cottise-extra-2)))

(defn render-fess-cottise [cottise-1-key cottise-2-key next-cottise-key
                           path environment context & {:keys [offset-y-fn
                                                              alignment
                                                              swap-lines?]}]
  (let [cottise-path (conj path :cottising cottise-1-key)
        cottise-2-path (conj path :cottising cottise-2-key)]
    (when (interface/get-raw-data cottise-path context)
      (let [line (interface/get-sanitized-data (conj cottise-path :line) context)
            opposite-line (interface/get-sanitized-data (conj cottise-path :opposite-line) context)
            [line opposite-line] (if swap-lines?
                                   [opposite-line line]
                                   [line opposite-line])
            thickness (interface/get-sanitized-data (conj cottise-path :thickness) context)
            distance (interface/get-sanitized-data (conj cottise-path :distance) context)
            outline? (interface/get-sanitized-data (conj cottise-path :outline?) context)]
        [ordinary-interface/render-ordinary
         [:context :cottise]
         path
         environment
         (assoc
          context
          :cottise {:type :heraldry.ordinary.type/fess
                    :field (interface/get-raw-data (conj cottise-path :field) context)
                    :line line
                    :opposite-line opposite-line
                    :geometry {:size thickness}
                    :cottising {next-cottise-key (interface/get-raw-data cottise-2-path context)}
                    :origin {:point :fess
                             :offset-y [:force (offset-y-fn
                                                (-> environment :points :fess :y)
                                                distance)]
                             :alignment alignment}
                    :outline? outline?})]))))

(defn render-pale-cottise [cottise-1-key cottise-2-key next-cottise-key
                           path environment context & {:keys [offset-x-fn
                                                              alignment
                                                              swap-lines?]}]
  (let [cottise-path (conj path :cottising cottise-1-key)
        cottise-2-path (conj path :cottising cottise-2-key)]
    (when (interface/get-raw-data cottise-path context)
      (let [line (interface/get-sanitized-data (conj cottise-path :line) context)
            opposite-line (interface/get-sanitized-data (conj cottise-path :opposite-line) context)
            [line opposite-line] (if swap-lines?
                                   [opposite-line line]
                                   [line opposite-line])
            thickness (interface/get-sanitized-data (conj cottise-path :thickness) context)
            distance (interface/get-sanitized-data (conj cottise-path :distance) context)
            outline? (interface/get-sanitized-data (conj cottise-path :outline?) context)]
        [ordinary-interface/render-ordinary
         [:context :cottise]
         path
         environment
         (assoc
          context
          :cottise {:type :heraldry.ordinary.type/pale
                    :field (interface/get-raw-data (conj cottise-path :field) context)
                    :line line
                    :opposite-line opposite-line
                    :geometry {:size thickness}
                    :cottising {next-cottise-key (interface/get-raw-data cottise-2-path context)}
                    :origin {:point :fess
                             :offset-x [:force (offset-x-fn
                                                (-> environment :points :fess :y)
                                                distance)]
                             :alignment alignment}
                    :outline? outline?})]))))

(defn render-bend-cottise [cottise-1-key cottise-2-key next-cottise-key
                           path environment context & {:keys [distance-fn
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
  (let [cottise-path (conj path :cottising cottise-1-key)
        cottise-2-path (conj path :cottising cottise-2-key)]
    (when (interface/get-raw-data cottise-path context)
      (let [line (interface/get-sanitized-data (conj cottise-path :line) context)
            opposite-line (interface/get-sanitized-data (conj cottise-path :opposite-line) context)
            [line opposite-line] (if swap-lines?
                                   [opposite-line line]
                                   [line opposite-line])
            thickness (interface/get-sanitized-data (conj cottise-path :thickness) context)
            distance (interface/get-sanitized-data (conj cottise-path :distance) context)
            effective-distance (distance-fn distance thickness)
            point-offset (v/* direction-orthogonal effective-distance)
            new-center-point (v/+ center-point point-offset)
            fess-offset (v/- new-center-point (-> environment :points :fess))
            outline? (interface/get-sanitized-data (conj cottise-path :outline?) context)]
        [ordinary-interface/render-ordinary
         [:context :cottise]
         path
         environment
         (-> context
             (assoc
              :cottise {:type (if sinister?
                                :heraldry.ordinary.type/bend-sinister
                                :heraldry.ordinary.type/bend)
                        :field (interface/get-raw-data (conj cottise-path :field) context)
                        :line line
                        :opposite-line opposite-line
                        :geometry {:size thickness}
                        :cottising {next-cottise-key (interface/get-raw-data cottise-2-path context)}
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
             (assoc :override-middle-real-end (middle-real-end-fn point-offset)))]))))

(defn render-chevron-cottise [cottise-1-key cottise-2-key next-cottise-key
                              path environment context & {:keys [distance-fn
                                                                 alignment
                                                                 swap-lines?
                                                                 width
                                                                 height
                                                                 joint-angle
                                                                 chevron-angle
                                                                 corner-point]}]
  (let [cottise-path (conj path :cottising cottise-1-key)
        cottise-2-path (conj path :cottising cottise-2-key)]
    (when (interface/get-raw-data cottise-path context)
      (let [line (interface/get-sanitized-data (conj cottise-path :line) context)
            opposite-line (interface/get-sanitized-data (conj cottise-path :opposite-line) context)
            [line opposite-line] (if swap-lines?
                                   [opposite-line line]
                                   [line opposite-line])
            thickness (interface/get-sanitized-data (conj cottise-path :thickness) context)
            distance (interface/get-sanitized-data (conj cottise-path :distance) context)
            half-joint-angle (/ joint-angle 2)
            half-joint-angle-rad (-> half-joint-angle
                                     (/ 180)
                                     (* Math/PI))
            effective-distance (distance-fn distance half-joint-angle-rad)
            point-offset (-> (v/v effective-distance 0)
                             (v/rotate chevron-angle)
                             (v/+ corner-point))
            fess-offset (v/- point-offset (-> environment :points :fess))
            outline? (interface/get-sanitized-data (conj cottise-path :outline?) context)]
        [ordinary-interface/render-ordinary
         [:context :cottise]
         path
         environment
         (-> context
             (assoc
              :cottise {:type :heraldry.ordinary.type/chevron
                        :field (interface/get-raw-data (conj cottise-path :field) context)
                        :line line
                        :opposite-line opposite-line
                        :geometry {:size thickness}
                        :cottising {next-cottise-key (interface/get-raw-data cottise-2-path context)}
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
                        :outline? outline?}))]))))
