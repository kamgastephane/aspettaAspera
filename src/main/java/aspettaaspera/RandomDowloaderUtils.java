package aspettaaspera;


public  final class RandomDowloaderUtils {

    private RandomDowloaderUtils()
    {

    }

    public static boolean IsNullOrWhiteSpace(String input)
    {
        return input == null || input.trim().isEmpty();
    }

}
