(ns heraldicon.heraldry.cottising
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]))

(defn add-cottise-options [options key context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside))]
    (assoc options
           key
           {:line (assoc line-style :ui/label :string.entity/line)
            :opposite-line (assoc opposite-line-style :ui/label :string.entity/opposite-line)
            :distance {:type :option.type/range
                       :min -10
                       :max 20
                       :default 2
                       :ui/label :string.option/distance
                       :ui/step 0.1}
            :thickness {:type :option.type/range
                        :min 0.1
                        :max 20
                        :default 2
                        :ui/label :string.option/thickness
                        :ui/step 0.1}
            :outline? {:type :option.type/boolean
                       :default false
                       :ui/label :string.charge.tincture-modifier.special/outline}
            :ui/element :ui.element/cottising})))

(defn add-cottising [context num]
  (let [cottising-context (c/++ context :cottising)]
    (cond-> {}
      (>= num 1) (->
                   (add-cottise-options :cottise-1 (c/++ cottising-context :cottise-1))
                   (add-cottise-options :cottise-2 (c/++ cottising-context :cottise-2)))
      (>= num 2) (->
                   (add-cottise-options :cottise-opposite-1 (c/++ cottising-context :cottise-opposite-1))
                   (add-cottise-options :cottise-opposite-2 (c/++ cottising-context :cottise-opposite-2)))
      (>= num 3) (->
                   (add-cottise-options :cottise-extra-1 (c/++ cottising-context :cottise-extra-1))
                   (add-cottise-options :cottise-extra-2 (c/++ cottising-context :cottise-extra-2))))))

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
      [ordinary.interface/render-ordinary
       (-> context
           (c/<< :path [:context :cottise])
           (assoc :cottise {:type :heraldry.ordinary.type/fess
                            :field (interface/get-raw-data (c/++ context :field))
                            :line line
                            :opposite-line opposite-line
                            :geometry {:size thickness}
                            :cottising {next-cottise-key (interface/get-raw-data
                                                          (-> context
                                                              c/--
                                                              (c/++ cottise-2-key)))}
                            :anchor {:point :fess
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
      [ordinary.interface/render-ordinary
       (-> context
           (c/<< :path [:context :cottise])
           (assoc :cottise {:type :heraldry.ordinary.type/pale
                            :field (interface/get-raw-data (c/++ context :field))
                            :line line
                            :opposite-line opposite-line
                            :geometry {:size thickness}
                            :cottising {next-cottise-key (interface/get-raw-data
                                                          (-> context
                                                              c/--
                                                              (c/++ cottise-2-key)))}
                            :anchor {:point :fess
                                     :offset-x [:force (offset-x-fn
                                                        (-> environment :points :fess :x)
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
      [ordinary.interface/render-ordinary
       (-> context
           (c/<< :path [:context :cottise])
           (assoc :cottise {:type (if sinister?
                                    :heraldry.ordinary.type/bend-sinister
                                    :heraldry.ordinary.type/bend)
                            :field (interface/get-raw-data (c/++ context :field))
                            :line line
                            :opposite-line opposite-line
                            :geometry {:size thickness}
                            :cottising {next-cottise-key (interface/get-raw-data
                                                          (-> context
                                                              c/--
                                                              (c/++ cottise-2-key)))}
                            :anchor {:point :fess
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
                            :orientation {:point :angle
                                          :angle angle}
                            :outline? outline?}
                  :override-center-point new-center-point
                  :override-middle-real-start (middle-real-start-fn point-offset)
                  :override-middle-real-end (middle-real-end-fn point-offset)))])))

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
          point-offset (-> (v/Vector. effective-distance 0)
                           (v/rotate chevron-angle)
                           (v/add corner-point))
          fess-offset (v/sub point-offset (-> environment :points :fess))
          outline? (interface/get-sanitized-data (c/++ context :outline?))]
      [ordinary.interface/render-ordinary
       (-> context
           (c/<< :path [:context :cottise])
           (assoc :cottise {:type :heraldry.ordinary.type/chevron
                            :field (interface/get-raw-data (c/++ context :field))
                            :line line
                            :opposite-line opposite-line
                            :geometry {:size thickness}
                            :cottising {next-cottise-key (interface/get-raw-data
                                                          (-> context
                                                              c/--
                                                              (c/++ cottise-2-key)))}
                            :anchor {:point :fess
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
                            :orientation {:point :angle
                                          :angle [:force half-joint-angle]}
                            :origin {:point :angle
                                     :angle [:force (- chevron-angle 90)]}
                            :outline? outline?}))])))

; :heraldry/cottise is a special case, it's a UI component, but not a :heraldry.options/root,
; because it is a child of the parent :cottising

(defmethod interface/options-subscriptions :heraldry/cottise [_context]
  #{})

(defmethod interface/options :heraldry/cottise [_context]
  nil)
