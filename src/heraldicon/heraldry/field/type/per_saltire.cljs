(ns heraldicon.heraldry.field.type.per-saltire
  (:require
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.field.shared :as shared]
   [heraldicon.svg.infinity :as infinity]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.orientation :as orientation]
   [heraldicon.render.outline :as outline]
   [heraldicon.heraldry.shared.saltire :as saltire]
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.path :as path]))

(def field-type :heraldry.field.type/per-saltire)

(defmethod field.interface/display-name field-type [_] :string.field.type/per-saltire)

(defmethod field.interface/part-names field-type [_] ["chief" "dexter" "sinister" "base"])

(defmethod interface/options field-type [context]
  (let [line-style (-> (line/options (c/++ context :line)
                                     :fimbriation? false)
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil))
        opposite-line-style (-> (line/options (c/++ context :opposite-line)
                                              :fimbriation? false
                                              :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil))
        orientation-point-option {:type :choice
                                  :choices [[:string.option.point-choice/top-left :top-left]
                                            [:string.option.point-choice/top-right :top-right]
                                            [:string.option.point-choice/bottom-left :bottom-left]
                                            [:string.option.point-choice/bottom-right :bottom-right]
                                            [:string.option.orientation-point-choice/angle :angle]]
                                  :default :top-left
                                  :ui {:label :string.option/point}}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    ;; TODO: perhaps there should be anchor options for the corners?
    ;; so one can align fro top-left to bottom-right
    {:anchor {:point {:type :choice
                      :choices [[:string.option.point-choice/chief :chief]
                                [:string.option.point-choice/base :base]
                                [:string.option.point-choice/fess :fess]
                                [:string.option.point-choice/dexter :dexter]
                                [:string.option.point-choice/sinister :sinister]
                                [:string.option.point-choice/honour :honour]
                                [:string.option.point-choice/nombril :nombril]]
                      :default :fess
                      :ui {:label :string.option/point}}
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
                                              :min 10
                                              :max 80
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
     :line line-style
     :opposite-line opposite-line-style}))

(defmethod field.interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        {anchor-point :real-anchor
         orientation-point :real-orientation} (orientation/calculate-anchor-and-orientation
                                               environment
                                               anchor
                                               orientation
                                               0
                                               nil)
        [relative-top-left relative-top-right
         relative-bottom-left relative-bottom-right] (saltire/arm-diagonals anchor-point orientation-point)
        diagonal-top-left (v/add anchor-point relative-top-left)
        diagonal-top-right (v/add anchor-point relative-top-right)
        diagonal-bottom-left (v/add anchor-point relative-bottom-left)
        diagonal-bottom-right (v/add anchor-point relative-bottom-right)
        intersection-top-left (v/find-first-intersection-of-ray anchor-point diagonal-top-left environment)
        intersection-top-right (v/find-first-intersection-of-ray anchor-point diagonal-top-right environment)
        intersection-bottom-left (v/find-first-intersection-of-ray anchor-point diagonal-bottom-left environment)
        intersection-bottom-right (v/find-first-intersection-of-ray anchor-point diagonal-bottom-right environment)
        arm-length (->> [intersection-top-left
                         intersection-top-right
                         intersection-bottom-left
                         intersection-bottom-right]
                        (map #(-> %
                                  (v/sub anchor-point)
                                  v/abs))
                        (apply max))
        line (-> line
                 (dissoc :fimbriation))
        {line-top-left :line
         line-top-left-start :line-start} (line/create line
                                                       anchor-point diagonal-top-left
                                                       :reversed? true
                                                       :real-start 0
                                                       :real-end arm-length
                                                       :context context
                                                       :environment environment)
        {line-top-right :line
         line-top-right-start :line-start} (line/create opposite-line
                                                        anchor-point diagonal-top-right
                                                        :flipped? true
                                                        :mirrored? true
                                                        :real-start 0
                                                        :real-end arm-length
                                                        :context context
                                                        :environment environment)
        {line-bottom-right :line
         line-bottom-right-start :line-start} (line/create line
                                                           anchor-point diagonal-bottom-right
                                                           :reversed? true
                                                           :real-start 0
                                                           :real-end arm-length
                                                           :context context
                                                           :environment environment)
        {line-bottom-left :line
         line-bottom-left-start :line-start} (line/create opposite-line
                                                          anchor-point diagonal-bottom-left
                                                          :flipped? true
                                                          :mirrored? true
                                                          :real-start 0
                                                          :real-end arm-length
                                                          :context context
                                                          :environment environment)
        ;; TODO: sub fields need better environment determination, especially with an adjusted anchor,
        ;; the resulting environments won't be very well centered
        parts [[["M" (v/add diagonal-top-left
                            line-top-left-start)
                 (path/stitch line-top-left)
                 "L" anchor-point
                 (path/stitch line-top-right)
                 (infinity/path :counter-clockwise
                                [:right :left]
                                [(v/add diagonal-top-right
                                        line-top-left-start)
                                 (v/add diagonal-top-left
                                        line-top-left-start)])
                 "z"]
                [intersection-top-left
                 intersection-top-right
                 anchor-point]]

               [["M" (v/add diagonal-top-left
                            line-top-left-start)
                 (path/stitch line-top-left)
                 "L" anchor-point
                 (path/stitch line-bottom-left)
                 (infinity/path :clockwise
                                [:left :left]
                                [(v/add diagonal-bottom-left
                                        line-bottom-left-start)
                                 (v/add diagonal-top-left
                                        line-top-left-start)])
                 "z"]
                [intersection-top-left
                 intersection-bottom-left
                 anchor-point]]

               [["M" (v/add diagonal-bottom-right
                            line-bottom-right-start)
                 (path/stitch line-bottom-right)
                 "L" anchor-point
                 (path/stitch line-top-right)
                 (infinity/path :clockwise
                                [:right :right]
                                [(v/add diagonal-top-right
                                        line-top-right-start)
                                 (v/add diagonal-bottom-right
                                        line-bottom-right-start)])
                 "z"]
                [intersection-top-right
                 intersection-bottom-right
                 anchor-point]]

               [["M" (v/add diagonal-bottom-right
                            line-bottom-right-start)
                 (path/stitch line-bottom-right)
                 "L" anchor-point
                 (path/stitch line-bottom-left)
                 (infinity/path :counter-clockwise
                                [:left :right]
                                [(v/add diagonal-bottom-left
                                        line-bottom-left-start)
                                 (v/add diagonal-bottom-right
                                        line-bottom-right-start)])
                 "z"]
                [intersection-bottom-left
                 intersection-bottom-right
                 anchor-point]]]]

    [:<>
     [shared/make-subfields
      context parts
      [:all
       [(path/make-path
         ["M" (v/add anchor-point
                     line-bottom-left-start)
          (path/stitch line-bottom-left)])]
       [(path/make-path
         ["M" (v/add diagonal-bottom-right
                     line-bottom-right-start)
          (path/stitch line-bottom-right)])]
       nil]
      environment]
     (when outline?
       [:g (outline/style context)
        [:path {:d (path/make-path
                    ["M" (v/add diagonal-top-left
                                line-top-left-start)
                     (path/stitch line-top-left)])}]
        [:path {:d (path/make-path
                    ["M" (v/add anchor-point
                                line-top-right-start)
                     (path/stitch line-top-right)])}]
        [:path {:d (path/make-path
                    ["M" (v/add diagonal-bottom-right
                                line-bottom-right-start)
                     (path/stitch line-bottom-right)])}]
        [:path {:d (path/make-path
                    ["M" (v/add anchor-point
                                line-bottom-left-start)
                     (path/stitch line-bottom-left)])}]])]))
