(ns heraldry.coat-of-arms.charge.core
  (:require [clojure.string :as s]
            [heraldry.coat-of-arms.charge.interface :as charge-interface]
            [heraldry.frontend.util :as util]
            [heraldry.interface :as interface]
            [re-frame.core :as rf]))

(defmethod interface/render-component :heraldry.component/charge [path parent-path environment context]
  [charge-interface/render-charge path parent-path environment context])

(defn title [path]
  (let [charge-type @(rf/subscribe [:get-value (conj path :type)])
        attitude (or @(rf/subscribe [:get-value (conj path :attitude)])
                     :none)
        facing (or @(rf/subscribe [:get-value (conj path :facing)])
                   :none)]
    (s/join " " [(util/translate-cap-first charge-type)
                 (when-not (= attitude :none)
                   (util/translate attitude))
                 (when-not (#{:none :to-dexter} facing)
                   (util/translate facing))])))
