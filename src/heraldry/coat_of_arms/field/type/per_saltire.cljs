(ns heraldry.coat-of-arms.field.type.per-saltire
  (:require
   [heraldry.coat-of-arms.angle :as angle]
   [heraldry.coat-of-arms.field.interface :as field-interface]
   [heraldry.coat-of-arms.field.shared :as shared]
   [heraldry.coat-of-arms.infinity :as infinity]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.coat-of-arms.outline :as outline]
   [heraldry.coat-of-arms.shared.saltire :as saltire]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]
   [heraldry.options :as options]
   [heraldry.strings :as strings]))

(def field-type :heraldry.field.type/per-saltire)

(defmethod field-interface/display-name field-type [_] {:en "Per saltire"
                                                        :de "Schräggeviert"})

(defmethod field-interface/part-names field-type [_] ["chief" "dexter" "sinister" "base"])

(defmethod interface/options field-type [context]
  (let [line-data (interface/get-raw-data (c/++ context :line))
        opposite-line-data (interface/get-raw-data (c/++ context :opposite-line))
        line-style (-> (line/options line-data)
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil)
                       (dissoc :fimbriation))
        sanitized-line (options/sanitize line-data line-style)
        opposite-line-style (-> (line/options opposite-line-data :inherited sanitized-line)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil)
                                (dissoc :fimbriation)
                                (update :ui assoc :label strings/opposite-line))
        anchor-point-option {:type :choice
                             :choices [[strings/top-left :top-left]
                                       [strings/top-right :top-right]
                                       [strings/bottom-left :bottom-left]
                                       [strings/bottom-right :bottom-right]
                                       [strings/angle :angle]]
                             :default :top-left
                             :ui {:label strings/point}}
        current-anchor-point (options/get-value
                              (interface/get-raw-data (c/++ context :anchor :point))
                              anchor-point-option)]
    ;; TODO: perhaps there should be origin options for the corners?
    ;; so one can align fro top-left to bottom-right
    {:origin {:point {:type :choice
                      :choices [[strings/chief-point :chief]
                                [strings/base-point :base]
                                [strings/fess-point :fess]
                                [strings/dexter-point :dexter]
                                [strings/sinister-point :sinister]
                                [strings/honour-point :honour]
                                [strings/nombril-point :nombril]]
                      :default :fess
                      :ui {:label strings/point}}
              :offset-x {:type :range
                         :min -45
                         :max 45
                         :default 0
                         :ui {:label strings/offset-x
                              :step 0.1}}
              :offset-y {:type :range
                         :min -45
                         :max 45
                         :default 0
                         :ui {:label strings/offset-y
                              :step 0.1}}
              :ui {:label strings/origin
                   :form-type :position}}
     :anchor (cond-> {:point anchor-point-option
                      :ui {:label strings/anchor
                           :form-type :position}}

               (= current-anchor-point
                  :angle) (assoc :angle {:type :range
                                         :min 10
                                         :max 80
                                         :default 45
                                         :ui {:label strings/angle}})

               (not= current-anchor-point
                     :angle) (assoc :offset-x {:type :range
                                               :min -45
                                               :max 45
                                               :default 0
                                               :ui {:label strings/offset-x
                                                    :step 0.1}}
                                    :offset-y {:type :range
                                               :min -45
                                               :max 45
                                               :default 0
                                               :ui {:label strings/offset-y
                                                    :step 0.1}}))
     :line line-style
     :opposite-line opposite-line-style
     :outline? options/plain-outline?-option}))

(defmethod field-interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
        origin (interface/get-sanitized-data (c/++ context :origin))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     0
                                     nil)
        [relative-top-left relative-top-right
         relative-bottom-left relative-bottom-right] (saltire/arm-diagonals origin-point anchor-point)
        diagonal-top-left (v/add origin-point relative-top-left)
        diagonal-top-right (v/add origin-point relative-top-right)
        diagonal-bottom-left (v/add origin-point relative-bottom-left)
        diagonal-bottom-right (v/add origin-point relative-bottom-right)
        intersection-top-left (v/find-first-intersection-of-ray origin-point diagonal-top-left environment)
        intersection-top-right (v/find-first-intersection-of-ray origin-point diagonal-top-right environment)
        intersection-bottom-left (v/find-first-intersection-of-ray origin-point diagonal-bottom-left environment)
        intersection-bottom-right (v/find-first-intersection-of-ray origin-point diagonal-bottom-right environment)
        arm-length (->> [intersection-top-left
                         intersection-top-right
                         intersection-bottom-left
                         intersection-bottom-right]
                        (map #(-> %
                                  (v/sub origin-point)
                                  v/abs))
                        (apply max))
        line (-> line
                 (dissoc :fimbriation))
        {line-top-left :line
         line-top-left-start :line-start} (line/create line
                                                       origin-point diagonal-top-left
                                                       :reversed? true
                                                       :real-start 0
                                                       :real-end arm-length
                                                       :context context
                                                       :environment environment)
        {line-top-right :line
         line-top-right-start :line-start} (line/create opposite-line
                                                        origin-point diagonal-top-right
                                                        :flipped? true
                                                        :mirrored? true
                                                        :real-start 0
                                                        :real-end arm-length
                                                        :context context
                                                        :environment environment)
        {line-bottom-right :line
         line-bottom-right-start :line-start} (line/create line
                                                           origin-point diagonal-bottom-right
                                                           :reversed? true
                                                           :real-start 0
                                                           :real-end arm-length
                                                           :context context
                                                           :environment environment)
        {line-bottom-left :line
         line-bottom-left-start :line-start} (line/create opposite-line
                                                          origin-point diagonal-bottom-left
                                                          :flipped? true
                                                          :mirrored? true
                                                          :real-start 0
                                                          :real-end arm-length
                                                          :context context
                                                          :environment environment)
        ;; TODO: sub fields need better environment determination, especially with an adjusted origin,
        ;; the resulting environments won't be very well centered
        parts [[["M" (v/add diagonal-top-left
                            line-top-left-start)
                 (path/stitch line-top-left)
                 "L" origin-point
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
                 origin-point]]

               [["M" (v/add diagonal-top-left
                            line-top-left-start)
                 (path/stitch line-top-left)
                 "L" origin-point
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
                 origin-point]]

               [["M" (v/add diagonal-bottom-right
                            line-bottom-right-start)
                 (path/stitch line-bottom-right)
                 "L" origin-point
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
                 origin-point]]

               [["M" (v/add diagonal-bottom-right
                            line-bottom-right-start)
                 (path/stitch line-bottom-right)
                 "L" origin-point
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
                 origin-point]]]]

    [:<>
     [shared/make-subfields
      context parts
      [:all
       [(path/make-path
         ["M" (v/add origin-point
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
                    ["M" (v/add origin-point
                                line-top-right-start)
                     (path/stitch line-top-right)])}]
        [:path {:d (path/make-path
                    ["M" (v/add diagonal-bottom-right
                                line-bottom-right-start)
                     (path/stitch line-bottom-right)])}]
        [:path {:d (path/make-path
                    ["M" (v/add origin-point
                                line-bottom-left-start)
                     (path/stitch line-bottom-left)])}]])]))
