(ns heraldicon.coat-of-arms.field.type.per-bend
  (:require
   [heraldicon.coat-of-arms.orientation :as orientation]
   [heraldicon.coat-of-arms.field.interface :as field.interface]
   [heraldicon.coat-of-arms.field.shared :as shared]
   [heraldicon.coat-of-arms.infinity :as infinity]
   [heraldicon.coat-of-arms.line.core :as line]
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [heraldicon.math.svg.path :as path]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]))

(def field-type :heraldry.field.type/per-bend)

(defmethod field.interface/display-name field-type [_] :string.field.type/per-bend)

(defmethod field.interface/part-names field-type [_] ["chief" "base"])

(defmethod interface/options field-type [context]
  (let [line-style (line/options (c/++ context :line))
        anchor-point-option {:type :choice
                             :choices [[:string.option.point-choice/fess :fess]
                                       [:string.option.point-choice/chief :chief]
                                       [:string.option.point-choice/base :base]
                                       [:string.option.point-choice/honour :honour]
                                       [:string.option.point-choice/nombril :nombril]
                                       [:string.option.point-choice/top-left :top-left]
                                       [:string.option.point-choice/bottom-right :bottom-right]]
                             :default :top-left
                             :ui {:label :string.option/point}}
        current-anchor-point (options/get-value
                              (interface/get-raw-data (c/++ context :anchor :point))
                              anchor-point-option)
        orientation-point-option {:type :choice
                                  :choices (case current-anchor-point
                                             :top-left [[:string.option.point-choice/fess :fess]
                                                        [:string.option.point-choice/chief :chief]
                                                        [:string.option.point-choice/base :base]
                                                        [:string.option.point-choice/honour :honour]
                                                        [:string.option.point-choice/nombril :nombril]
                                                        [:string.option.point-choice/bottom-right :bottom-right]
                                                        [:string.option.orientation-point-choice/angle :angle]]
                                             :bottom-right [[:string.option.point-choice/fess :fess]
                                                            [:string.option.point-choice/chief :chief]
                                                            [:string.option.point-choice/base :base]
                                                            [:string.option.point-choice/honour :honour]
                                                            [:string.option.point-choice/nombril :nombril]
                                                            [:string.option.point-choice/top-left :top-left]
                                                            [:string.option.orientation-point-choice/angle :angle]]
                                             [[:string.option.point-choice/top-left :top-left]
                                              [:string.option.point-choice/bottom-right :bottom-right]
                                              [:string.option.orientation-point-choice/angle :angle]])
                                  :default (case current-anchor-point
                                             :top-left :fess
                                             :bottom-right :fess
                                             :top-left)
                                  :ui {:label :string.option/point}}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    {:anchor {:point anchor-point-option
              :offset-x {:type :range
                         :min -45
                         :max 45
                         :default 0
                         :ui {:label :string.option/offset-x
                              :step 0.1}}
              :offset-y {:type :range
                         :min -45
                         :max 45
                         :default 0
                         :ui {:label :string.option/offset-y
                              :step 0.1}}
              :ui {:label :string.option/anchor
                   :form-type :position}}
     :orientation (cond-> {:point orientation-point-option
                           :ui {:label :string.option/orientation
                                :form-type :position}}

                    (= current-orientation-point
                       :angle) (assoc :angle {:type :range
                                              :min 0
                                              :max 360
                                              :default 45
                                              :ui {:label :string.option/angle}})

                    (not= current-orientation-point
                          :angle) (assoc :offset-x {:type :range
                                                    :min -45
                                                    :max 45
                                                    :default 0
                                                    :ui {:label :string.option/offset-x
                                                         :step 0.1}}
                                         :offset-y {:type :range
                                                    :min -45
                                                    :max 45
                                                    :default 0
                                                    :ui {:label :string.option/offset-y
                                                         :step 0.1}}))
     :line line-style}))

(defmethod field.interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        top-right (:top-right points)
        bottom-left (:bottom-left points)
        {anchor-point :real-anchor
         orientation-point :real-orientation} (orientation/calculate-anchor-and-orientation
                                               environment
                                               anchor
                                               orientation
                                               0
                                               nil)
        direction (v/sub orientation-point anchor-point)
        direction (v/normal (v/v (-> direction :x Math/abs)
                                 (-> direction :y Math/abs)))
        initial-diagonal-start (-> direction
                                   (v/mul -1000)
                                   (v/add anchor-point))
        initial-diagonal-end (-> direction
                                 (v/mul 1000)
                                 (v/add anchor-point))
        [real-diagonal-start
         real-diagonal-end] (v/environment-intersections
                             initial-diagonal-start
                             initial-diagonal-end
                             environment)
        effective-width (or (:width line) 1)
        effective-width (cond-> effective-width
                          (:spacing line) (+ (* (:spacing line) effective-width)))
        required-extra-length (-> 30
                                  (/ effective-width)
                                  Math/ceil
                                  inc
                                  (* effective-width))
        extra-dir (-> direction
                      (v/mul required-extra-length))
        diagonal-start (v/sub real-diagonal-start extra-dir)
        diagonal-end (v/add real-diagonal-end extra-dir)
        {line-one :line
         line-one-start :line-start
         line-one-end :line-end
         :as line-one-data} (line/create line
                                         diagonal-start diagonal-end
                                         :context context
                                         :environment environment)
        parts [[["M" (v/add diagonal-start
                            line-one-start)
                 (path/stitch line-one)
                 (infinity/path :counter-clockwise
                                [:right :top]
                                [(v/add diagonal-end
                                        line-one-end)
                                 (v/add diagonal-start
                                        line-one-start)])
                 "z"]
                [real-diagonal-start
                 top-right
                 real-diagonal-end]]

               [["M" (v/add diagonal-start
                            line-one-start)
                 (path/stitch line-one)
                 (infinity/path :clockwise
                                [:right :top]
                                [(v/add diagonal-end
                                        line-one-end)
                                 (v/add diagonal-start
                                        line-one-start)])
                 "z"]
                [real-diagonal-start
                 real-diagonal-end
                 bottom-left]]]]
    [:<>
     [shared/make-subfields
      context parts
      [:all nil]
      environment]
     [line/render line [line-one-data] diagonal-start outline? context]]))
