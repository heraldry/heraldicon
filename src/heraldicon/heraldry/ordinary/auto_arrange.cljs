(ns heraldicon.heraldry.ordinary.auto-arrange
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.ordinary.post-process :as post-process]
   [heraldicon.interface :as interface]))

(defn- spacing-factor [ordinary-type]
  (case ordinary-type
    :heraldry.ordinary.type/orle 0.75
    0.3333333333))

(defn- size [ordinary-type num-ordinaries]
  (let [[base-size min-size factor] (case ordinary-type
                                      :heraldry.ordinary.type/orle [5 2 0.8]
                                      :heraldry.ordinary.type/chevron [20 6 0.85]
                                      [25 7 0.85])]
    (max (* base-size (js/Math.pow factor (dec num-ordinaries)))
         min-size)))

(defn- spacing [ordinary-type num-ordinaries]
  (* (spacing-factor ordinary-type)
     (size ordinary-type num-ordinaries)))

(defn- get-auto-positioned-ordinaries [context ordinary-type]
  (let [num-elements (interface/get-list-size context)]
    (into []
          (comp
           (map #(c/++ context %))
           (filter (fn [component-context]
                     (and (= (interface/get-raw-data (c/++ component-context :type))
                             ordinary-type)
                          (case ordinary-type
                            :heraldry.ordinary.type/orle (= (or (interface/get-raw-data (c/++ component-context :positioning-mode))
                                                                :auto)
                                                            :auto)
                            (= (or (interface/get-raw-data (c/++ component-context :anchor :point))
                                   :auto)
                               :auto))))))
          (range num-elements))))

(defmethod interface/auto-ordinary-info :default [ordinary-type context]
  (let [ordinaries (get-auto-positioned-ordinaries (c/++ context :components) ordinary-type)
        num-ordinaries (count ordinaries)]
    {:ordinary-contexts ordinaries
     :num-ordinaries num-ordinaries
     :affected-paths (if (> num-ordinaries 1)
                       (into {}
                             (map-indexed (fn [index {:keys [path]}]
                                            [path index]))
                             ordinaries)
                       {})
     :default-size (size ordinary-type num-ordinaries)
     :default-spacing (spacing ordinary-type num-ordinaries)}))

(defn set-spacing-top [{:keys [context]
                        :as ordinary}]
  (assoc ordinary :spacing-top (interface/get-sanitized-data (c/++ context :anchor :spacing-top))))

(defn set-spacing-left [{:keys [context]
                         :as ordinary}]
  (assoc ordinary :spacing-left (interface/get-sanitized-data (c/++ context :anchor :spacing-left))))

(defn set-size [{:keys [context]
                 :as ordinary}]
  (assoc ordinary :size (interface/get-sanitized-data (c/++ context :geometry :size))))

(defn set-distance [{:keys [context]
                     :as ordinary}]
  (assoc ordinary :distance (interface/get-sanitized-data (c/++ context :distance))))

(defn set-thickness [{:keys [context]
                      :as ordinary}]
  (assoc ordinary :thickness (interface/get-sanitized-data (c/++ context :thickness))))

(defn set-line-data [{:keys [context line-length]
                      :as ordinary}]
  (let [{:keys [line
                opposite-line]} (post-process/line-properties {:line-length line-length} context)]
    (assoc ordinary
           :line line
           :opposite-line opposite-line)))

(defn set-cottise-data [{:keys [context line-length percentage-base]
                         :as ordinary}]
  (assoc ordinary
         :cottise-height (+ (cottising/cottise-height (c/++ context :cottising :cottise-1)
                                                      line-length percentage-base)
                            (cottising/cottise-height (c/++ context :cottising :cottise-2)
                                                      line-length percentage-base))
         :opposite-cottise-height (+ (cottising/cottise-height (c/++ context :cottising :cottise-opposite-1)
                                                               line-length percentage-base)
                                     (cottising/cottise-height (c/++ context :cottising :cottise-opposite-2)
                                                               line-length percentage-base))))
