(ns heraldry.coat-of-arms.charge.core
  (:require [clojure.string :as s]
            [heraldry.coat-of-arms.charge.interface :as charge-interface]
            [heraldry.frontend.util :as util]
            [heraldry.interface :as interface]))

(defmethod interface/render-component :heraldry.component/charge [path parent-path environment context]
  [charge-interface/render-charge path parent-path environment context])

(defn title [charge]
  (s/join " " [(-> charge :type util/translate-cap-first)
               (when (-> charge :attitude (not= :none))
                 (-> charge :attitude util/translate))
               (when (-> charge :facing #{:none :to-dexter} not)
                 (-> charge :facing util/translate))]))
