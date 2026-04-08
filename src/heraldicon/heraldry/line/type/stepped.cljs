(ns heraldicon.heraldry.line.type.stepped
  (:require
   [heraldicon.math.angle :as angle]))

(def pattern
  {:display-name :string.line.type/stepped
   :function (fn [{:keys [width flipped?]} {:keys [angle reversed?] :as _line-options}]
               (let [angle (or angle 0)
                     rad (angle/to-rad angle)
                     sin-a (Math/sin rad)
                     cos-a (Math/cos rad)
                     h-dx (* width cos-a cos-a)
                     h-dy (- (* width sin-a cos-a))
                     v-dx (* width sin-a sin-a)
                     v-dy (* width sin-a cos-a)
                     swap? (not= (boolean flipped?) (boolean reversed?))
                     y-factor (if flipped? -1 1)
                     [seg1-dx seg1-dy seg2-dx seg2-dy]
                     (if swap?
                       [v-dx (* y-factor v-dy) h-dx (* y-factor h-dy)]
                       [h-dx (* y-factor h-dy) v-dx (* y-factor v-dy)])]
                 {:pattern ["l" [seg1-dx seg1-dy]
                            "l" [seg2-dx seg2-dy]]
                  :min (min 0 seg1-dy)
                  :max (max 0 seg1-dy)}))})
