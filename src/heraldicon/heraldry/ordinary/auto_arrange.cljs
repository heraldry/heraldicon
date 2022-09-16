(ns heraldicon.heraldry.ordinary.auto-arrange
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.ordinary.post-process :as post-process]
   [heraldicon.interface :as interface]
   [heraldicon.math.core :as math]))

(defn size [num-ordinaries]
  (max (* 25 (js/Math.pow 0.75 (dec num-ordinaries)))
       7.5))

(defn margin [num-ordinaries]
  (max (* 10 (js/Math.pow 0.75 (dec num-ordinaries)))
       3))

(defn- get-auto-positioned-ordinaries [context ordinary-type]
  (let [num-elements (interface/get-list-size context)]
    (into []
          (comp
           (map (fn [idx]
                  {:context (c/++ context idx)}))
           (filter (fn [{component-context :context}]
                     (and (= (interface/get-raw-data (c/++ component-context :type))
                             ordinary-type)
                          (= (or (interface/get-raw-data (c/++ component-context :anchor :point))
                                 :auto)
                             :auto)))))
          (range num-elements))))

(defn num-auto-positioned-ordinaries [context ordinary-type]
  (count (get-auto-positioned-ordinaries context ordinary-type)))

(defn set-offset-x [{:keys [context percentage-base]
                     :as bar}]
  (assoc bar
         :offset-x (math/percent-of percentage-base
                                    (interface/get-sanitized-data (c/++ context :anchor :offset-x)))))

(defn set-offset-y [{:keys [context percentage-base]
                     :as bar}]
  (assoc bar
         :offset-y (math/percent-of percentage-base
                                    (interface/get-sanitized-data (c/++ context :anchor :offset-y)))))

(defn set-size [{:keys [context]
                 :as bar}]
  (update bar
          :size (fn [size]
                  (or (interface/get-raw-data (c/++ context :geometry :size))
                      size))))

(defn set-line-data [{:keys [context line-length]
                      :as bar}]
  (let [{:keys [line
                opposite-line]} (post-process/line-properties {:line-length line-length} context)]
    (assoc bar
           :line line
           :opposite-line opposite-line)))

(defn set-cottise-data [{:keys [context line-length percentage-base]
                         :as bar}]
  (assoc bar
         :cottise-height (+ (cottising/cottise-height (c/++ context :cottising :cottise-1)
                                                      line-length percentage-base)
                            (cottising/cottise-height (c/++ context :cottising :cottise-2)
                                                      line-length percentage-base))
         :opposite-cottise-height (+ (cottising/cottise-height (c/++ context :cottising :cottise-opposite-1)
                                                               line-length percentage-base)
                                     (cottising/cottise-height (c/++ context :cottising :cottise-opposite-2)
                                                               line-length percentage-base))))
