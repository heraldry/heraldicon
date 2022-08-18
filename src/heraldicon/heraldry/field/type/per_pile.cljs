(ns heraldicon.heraldry.field.type.per-pile
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.field.shared :as shared]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.shared.pile :as pile]
   [heraldicon.interface :as interface]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.infinity :as infinity]
   [heraldicon.svg.path :as path]))

(def field-type :heraldry.field.type/per-pile)

(defmethod field.interface/display-name field-type [_] :string.field.type/per-pile)

(defmethod field.interface/part-names field-type [_] nil)

(def ^:private size-mode-choices
  [[:string.option.size-mode-choice/thickness :thickness]
   [:string.option.size-mode-choice/angle :angle]])

(def size-mode-map
  (options/choices->map size-mode-choices))

(def ^:private orientation-type-choices
  [[:string.option.orientation-type-choice/edge :edge]
   [:string.option.orientation-type-choice/orientation-point :point]])

(def orientation-type-map
  (options/choices->map orientation-type-choices))

(defmethod field.interface/options field-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil))
        anchor-point-option {:type :option.type/choice
                             :choices (position/anchor-choices
                                       [:chief
                                        :base
                                        :dexter
                                        :sinister
                                        :hoist
                                        :fly
                                        :top-left
                                        :top
                                        :top-right
                                        :left
                                        :right
                                        :bottom-left
                                        :bottom
                                        :bottom-right])
                             :default :bottom
                             :ui/label :string.option/point}
        current-anchor-point (options/get-value
                              (interface/get-raw-data (c/++ context :anchor :point))
                              anchor-point-option)
        orientation-point-option {:type :option.type/choice
                                  :choices (position/orientation-choices
                                            (filter
                                             #(not= % current-anchor-point)
                                             [:top-left
                                              :top
                                              :top-right
                                              :left
                                              :center
                                              :right
                                              :bottom-left
                                              :bottom
                                              :bottom-right
                                              :fess
                                              :chief
                                              :base
                                              :dexter
                                              :sinister
                                              :honour
                                              :nombril
                                              :hoist
                                              :fly
                                              :angle]))
                                  :default :fess
                                  :ui/label :string.option/point}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)
        size-mode-option {:type :option.type/choice
                          :choices size-mode-choices
                          :default :thickness
                          :ui/label :string.option/size-mode
                          :ui/element :ui.element/radio-select}
        current-size-mode (options/get-value
                           (interface/get-raw-data (c/++ context :geometry :size-mode))
                           size-mode-option)]
    {:anchor {:point anchor-point-option
              :alignment {:type :option.type/choice
                          :choices position/alignment-choices
                          :default :middle
                          :ui/label :string.option/alignment
                          :ui/element :ui.element/radio-select}
              :offset-x {:type :option.type/range
                         :min -45
                         :max 45
                         :default 0
                         :ui/label :string.option/offset-x
                         :ui/step 0.1}
              :offset-y {:type :option.type/range
                         :min -45
                         :max 45
                         :default 0
                         :ui/label :string.option/offset-y
                         :ui/step 0.1}
              :ui/label :string.option/anchor
              :ui/element :ui.element/position}
     :orientation (cond-> {:point orientation-point-option
                           :ui/label :string.option/orientation
                           :ui/element :ui.element/position}

                    (= current-orientation-point
                       :angle) (assoc :angle {:type :option.type/range
                                              :min (cond
                                                     (#{:top-left
                                                        :top-right
                                                        :bottom-left
                                                        :bottom-right} current-anchor-point) 0
                                                     :else -90)
                                              :max 90
                                              :default (cond
                                                         (#{:top-left
                                                            :top-right
                                                            :bottom-left
                                                            :bottom-right} current-anchor-point) 45
                                                         :else 0)
                                              :ui/label :string.option/angle})

                    (not= current-orientation-point
                          :angle) (assoc :offset-x {:type :option.type/range
                                                    :min -45
                                                    :max 45
                                                    :default 0
                                                    :ui/label :string.option/offset-x
                                                    :ui/step 0.1}
                                         :offset-y {:type :option.type/range
                                                    :min -45
                                                    :max 45
                                                    :default 0
                                                    :ui/label :string.option/offset-y
                                                    :ui/step 0.1}))
     :line line-style
     :opposite-line opposite-line-style
     :geometry {:size-mode size-mode-option
                :size {:type :option.type/range
                       :min 5
                       :max 120
                       :default (case current-size-mode
                                  :thickness 75
                                  30)
                       :ui/label :string.option/size
                       :ui/step 0.1}
                :stretch {:type :option.type/range
                          :min 0.33
                          :max 2
                          :default 1
                          :ui/label :string.option/stretch
                          :ui/step 0.01}
                :ui/label :string.option/geometry
                :ui/element :ui.element/geometry}}))

(defmethod field.interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        geometry (interface/get-sanitized-data (c/++ context :geometry))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        orientation (assoc orientation :type :edge)
        geometry (assoc geometry :stretch 1)
        points (:points environment)
        top-left (:top-left points)
        top-right (:top-right points)
        bottom-left (:bottom-left points)
        bottom-right (:bottom-right points)
        thickness-base (if (#{:left :right} (:point anchor))
                         (:height environment)
                         (:width environment))
        {anchor-point :anchor
         point :point
         thickness :thickness} (pile/calculate-properties
                                environment
                                anchor
                                (cond-> orientation
                                  (#{:top-right
                                     :right
                                     :bottom-left} (:point anchor)) (update :angle #(when %
                                                                                      (- %))))
                                geometry
                                thickness-base
                                (case (:point anchor)
                                  :top-left 0
                                  :top 90
                                  :top-right 180
                                  :left 0
                                  :right 180
                                  :bottom-left 0
                                  :bottom -90
                                  :bottom-right 180
                                  0))
        {left-point :left
         right-point :right} (pile/diagonals anchor-point point thickness)
        intersection-left (last (v/environment-intersections point left-point environment))
        intersection-right (last (v/environment-intersections point right-point environment))
        end-left (-> intersection-left
                     (v/sub point)
                     v/abs)
        end-right (-> intersection-right
                      (v/sub point)
                      v/abs)
        end (max end-left end-right)
        line (-> line
                 (update-in [:fimbriation :thickness-1] (partial math/percent-of thickness-base))
                 (update-in [:fimbriation :thickness-2] (partial math/percent-of thickness-base)))
        {line-left :line
         line-left-start :line-start
         line-left-end :line-end
         :as line-left-data} (line/create line
                                          point left-point
                                          :reversed? true
                                          :real-start 0
                                          :real-end end
                                          :context context
                                          :environment environment)
        {line-right :line
         line-right-start :line-start
         line-right-end :line-end
         :as line-right-data} (line/create opposite-line
                                           point right-point
                                           :real-start 0
                                           :real-end end
                                           :context context
                                           :environment environment)
        parts [[["M" (v/add point
                            line-right-start)
                 (path/stitch line-right)
                 (infinity/path
                  :counter-clockwise
                  (cond
                    (#{:top-left
                       :top
                       :top-right} (:point anchor)) [:top :bottom]
                    (#{:left} (:point anchor)) [:left :right]
                    (#{:right} (:point anchor)) [:right :left]
                    (#{:bottom-left
                       :bottom
                       :bottom-right} (:point anchor)) [:bottom :top]
                    :else [:top :bottom])
                  [(v/add point
                          line-right-end)
                   (v/add point
                          line-right-start)])
                 "z"]
                ;; TODO: these fields inherit the whole parent
                ;; environment points, but it can probably be reduced
                [top-left top-right
                 bottom-left bottom-right]]

               [["M" (v/add left-point
                            line-left-start)
                 (path/stitch line-left)
                 (path/stitch line-right)
                 "z"]
                ;; TODO: these fields inherit the whole parent
                ;; environment points, but it can probably be reduced
                [top-left top-right
                 bottom-left bottom-right]]

               [["M" (v/add left-point
                            line-left-start)
                 (path/stitch line-left)
                 (infinity/path
                  :counter-clockwise
                  (cond
                    (#{:top-left
                       :top
                       :top-right} (:point anchor)) [:bottom :top]
                    (#{:left} (:point anchor)) [:right :left]
                    (#{:right} (:point anchor)) [:left :right]
                    (#{:bottom-left
                       :bottom
                       :bottom-right} (:point anchor)) [:top :bottom]
                    :else [:bottom :top])
                  [(v/add left-point
                          line-left-end)
                   (v/add left-point
                          line-left-start)])
                 "z"]
                ;; TODO: these fields inherit the whole parent
                ;; environment points, but it can probably be reduced
                [top-left top-right
                 bottom-left bottom-right]]]]
    [:<>
     [shared/make-subfields
      context parts
      [:all nil nil]
      environment]
     [line/render line [line-left-data
                        line-right-data] left-point outline? context]]))
