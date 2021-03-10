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

(defn set-line-defaults [options]
  (-> options
      (assoc-in [:fimbriation :alignment :default] :outside)))

(def default-options
  {:origin        position/default-options
   :anchor        (-> position/default-options
                      (update-in [:point :choices] conj ["Relative" :relative]))
   :diagonal-mode {:type    :choice
                   :default :top-left-origin}
   :line          (set-line-defaults line/default-options)
   :opposite-line (set-line-defaults line/default-options)
   :geometry      (-> geometry/default-options
                      (assoc-in [:size :min] 10)
                      (assoc-in [:size :max] 50)
                      (assoc-in [:size :default] 25)
                      (assoc :mirrored? nil)
                      (assoc :reversed? nil)
                      (assoc :stretch nil)
                      (assoc :rotation nil))})

(defn options [ordinary]
  (when ordinary
    (->
     (case (:type ordinary)
       :pale          (options/pick default-options
                                    [[:origin]
                                     [:line]
                                     [:opposite-line]
                                     [:geometry]]
                                    {[:offset-y] nil})
       :fess          (options/pick default-options
                                    [[:origin]
                                     [:line]
                                     [:opposite-line]
                                     [:geometry]]
                                    {[:offset-x] nil})
       :chief         (options/pick default-options
                                    [[:line]
                                     [:geometry]])
       :base          (options/pick default-options
                                    [[:line]
                                     [:geometry]])
       :bend          (options/pick default-options
                                    [[:origin]
                                     [:diagonal-mode]
                                     [:line]
                                     [:opposite-line]
                                     [:geometry]]
                                    {[:origin :point :choices] position/point-choices-y
                                     [:diagonal-mode :choices] (diagonal-mode-choices :bend)})
       :bend-sinister (options/pick default-options
                                    [[:origin]
                                     [:diagonal-mode]
                                     [:line]
                                     [:opposite-line]
                                     [:geometry]]
                                    {[:origin :point :choices] position/point-choices-y
                                     [:diagonal-mode :choices] (diagonal-mode-choices :bend-sinister)
                                     [:diagonal-mode :default] :top-right-origin})
       :chevron       (options/pick default-options
                                    [[:origin]
                                     [:anchor]
                                     [:diagonal-mode]
                                     [:line]
                                     [:opposite-line]
                                     [:geometry]]
                                    {[:diagonal-mode :choices]     (diagonal-mode-choices :chevron)
                                     [:diagonal-mode :default]     :forty-five-degrees
                                     [:line :offset :min]          0
                                     [:opposite-line :offset :min] 0})
       :saltire       (options/pick default-options
                                    [[:origin]
                                     [:diagonal-mode]
                                     [:line]
                                     [:geometry]]
                                    {[:diagonal-mode :choices]     (diagonal-mode-choices :saltire)
                                     [:line :offset :min]          0
                                     [:opposite-line :offset :min] 0})
       :cross         (options/pick default-options
                                    [[:origin]
                                     [:line]
                                     [:geometry]]
                                    {[:line :offset :min] 0}))
     (update-in [:line] (fn [line]
                          (when line
                            (set-line-defaults line))))
     (update-in [:opposite-line] (fn [opposite-line]
                                   (when opposite-line
                                     (set-line-defaults opposite-line)))))))

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
