(ns heraldry.coat-of-arms.ordinary.options
  (:require [heraldry.coat-of-arms.geometry :as geometry]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]

            [heraldry.util :as util]))

(defn diagonal-mode-choices [type]
  (let [options {:forty-five-degrees  "45°"
                 :top-left-origin     "Top-left to origin"
                 :top-right-origin    "Top-right to origin"
                 :bottom-left-origin  "Bottom-left to origin"
                 :bottom-right-origin "Bottom-right to origin"}]
    (->> type
         (get {:bend          [:forty-five-degrees
                               :top-left-origin]
               :bend-sinister [:forty-five-degrees
                               :top-right-origin]
               :chevron       [:forty-five-degrees
                               :bottom-left-origin
                               :bottom-right-origin]
               :saltire       [:forty-five-degrees
                               :top-left-origin
                               :top-right-origin
                               :bottom-left-origin
                               :bottom-right-origin]})
         (map (fn [key]
                [(get options key) key])))))

(defn diagonal-default [type]
  (or (get {:bend-sinister :top-right-origin
            :chevron       :forty-five-degrees} type)
      :top-left-origin))

(def default-options
  {:origin        position/default-options
   :diagonal-mode {:type    :choice
                   :default :top-left-origin}
   :line          line/default-options
   :opposite-line line/default-options
   :geometry      (-> geometry/default-options
                      (assoc-in [:size :min] 10)
                      (assoc-in [:size :default] 25)
                      (assoc :mirrored? nil)
                      (assoc :reversed? nil)
                      (assoc :stretch nil)
                      (assoc :rotation nil))})

(defn options [ordinary]
  (when ordinary
    (let [type (:type ordinary)]
      (->
       default-options
       (options/merge {:line (line/options (get-in ordinary [:line]))})
       (options/merge {:opposite-line (line/options (get-in ordinary [:opposite-line]))})
       (options/merge
        (->
         (get {:pale          {:origin        {:offset-y nil}
                               :diagonal-mode nil
                               :geometry      {:size {:max 50}}}
               :fess          {:origin        {:offset-x nil}
                               :diagonal-mode nil
                               :geometry      {:size {:max 50}}}
               :chief         {:origin        nil
                               :diagonal-mode nil
                               :opposite-line nil
                               :geometry      {:size {:max 50}}}
               :base          {:origin        nil
                               :opposite-line nil
                               :diagonal-mode nil
                               :geometry      {:size {:max 50}}}
               :bend          {:origin        {:point {:choices position/point-choices-y}}
                               :diagonal-mode {:choices (diagonal-mode-choices
                                                         :bend)}
                               :geometry      {:size {:max 50}}}
               :bend-sinister {:origin        {:point {:choices position/point-choices-y}}
                               :diagonal-mode {:choices (diagonal-mode-choices
                                                         :bend-sinister)
                                               :default :top-right-origin}
                               :geometry      {:size {:max 50}}}
               :chevron       {:diagonal-mode {:choices (diagonal-mode-choices
                                                         :chevron)
                                               :default :forty-five-degrees}
                               :line          {:offset {:min 0}}
                               :opposite-line {:offset {:min 0}}
                               :geometry      {:size {:max 30}}}
               :saltire       {:diagonal-mode {:choices (diagonal-mode-choices
                                                         :saltire)}
                               :line          {:offset {:min 0}}
                               :opposite-line nil
                               :geometry      {:size {:max 30}}}
               :cross         {:diagonal-mode nil
                               :line          {:offset {:min 0}}
                               :opposite-line nil
                               :geometry      {:size {:max 30}}}}
              type)))))))

(defn sanitize-opposite-line [ordinary line]
  (-> (options/sanitize
       (util/deep-merge-with (fn [_current-value new-value]
                               new-value) line
                             (into {}
                                   (filter (fn [[_ v]]
                                             (some? v))
                                           (:opposite-line ordinary))))
       (-> ordinary options :opposite-line))
      (assoc :flipped? (if (-> ordinary :opposite-line :flipped?)
                         (not (:flipped? line))
                         (:flipped? line)))))
