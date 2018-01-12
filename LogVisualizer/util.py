def sliding_window(sequence,winSize,step=1):
    numOfChunks = ((len(sequence)-winSize)/step)+1
    for i in range(0,numOfChunks*step,step):
        yield sequence[i:i+winSize]


# From NLTK
def string_edit_distance(s1, s2):
    """
    Calculate the Levenshtein edit-distance between two strings.
    The edit distance is the number of characters that need to be
    substituted, inserted, or deleted, to transform s1 into s2.  For
    example, transforming "rain" to "shine" requires three steps,
    consisting of two substitutions and one insertion:
    "rain" -> "sain" -> "shin" -> "shine".  These operations could have
    been done in other orders, but at least three steps are needed.

    :param s1, s2: The strings to be analysed
    :type s1: str
    :type s2: str
    :rtype int
    """

    def _edit_dist_init(len1, len2):
        lev = []
        for i in range(len1):
            lev.append([0] * len2)  # initialize 2-D array to zero
        for i in range(len1):
            lev[i][0] = i  # column 0: 0,1,2,3,4,...
        for j in range(len2):
            lev[0][j] = j  # row 0: 0,1,2,3,4,...
        return lev


    def _edit_dist_step(lev, i, j, c1, c2):
        a = lev[i - 1][j] + 1  # skipping s1[i]
        b = lev[i - 1][j - 1] + (c1 != c2)  # matching s1[i] with s2[j]
        c = lev[i][j - 1] + 1  # skipping s2[j]
        lev[i][j] = min(a, b, c)  # pick the cheapest

    # set up a 2-D array
    len1 = len(s1)
    len2 = len(s2)
    lev = _edit_dist_init(len1 + 1, len2 + 1)

    # iterate over the array
    for i in range(len1):
        for j in range(len2):
            _edit_dist_step(lev, i + 1, j + 1, s1[i], s2[j])
    return lev[len1][len2]


def average(lst):
    """
    :param lst: list
    :return: float
    """
    if not lst: return 0.0
    return 1.0*sum(lst) / len(lst)
